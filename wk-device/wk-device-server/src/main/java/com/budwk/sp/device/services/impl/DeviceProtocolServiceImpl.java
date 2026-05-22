package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceScriptRefreshMessageDTO;
import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolEncodeRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.providers.IDeviceProtocolDebugProvider;
import com.budwk.sp.device.services.DeviceProtocolService;
import com.budwk.sp.device.services.support.DeviceEntityRedisCacheService;
import com.budwk.sp.device.services.support.DeviceLocalScriptExecutionService;
import com.budwk.sp.device.support.DeviceScriptChannels;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.rpc.RpcException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.apache.dubbo.config.annotation.DubboReference;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceProtocolServiceImpl extends BaseServiceImpl<Device_protocol> implements DeviceProtocolService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DeviceEntityRedisCacheService deviceEntityRedisCacheService;
    private final DeviceLocalScriptExecutionService deviceLocalScriptExecutionService;
    @DubboReference(interfaceClass = IDeviceProtocolDebugProvider.class, check = false, lazy = true, retries = 0)
    private IDeviceProtocolDebugProvider deviceProtocolDebugProvider;

    public DeviceProtocolServiceImpl(Dao dao, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
                                     DeviceEntityRedisCacheService deviceEntityRedisCacheService,
                                     DeviceLocalScriptExecutionService deviceLocalScriptExecutionService) {
        super(dao);
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.deviceEntityRedisCacheService = deviceEntityRedisCacheService;
        this.deviceLocalScriptExecutionService = deviceLocalScriptExecutionService;
    }

    @Override
    public Device_protocol createProtocol(DeviceProtocolDTO dto, String operatorId, String tenantId) {
        checkCodeUnique(null, tenantId, dto.getCode());
        Device_protocol protocol = new Device_protocol();
        protocol.setTenantId(tenantId);
        protocol.setName(Strings.sNull(dto.getName()).trim());
        protocol.setCode(Strings.sNull(dto.getCode()).trim());
        protocol.setScriptType(dto.getScriptType());
        protocol.setScriptContent(dto.getScriptContent());
        protocol.setScriptVersion(Strings.sNull(dto.getScriptVersion()).trim());
        protocol.setDescription(Strings.sNull(dto.getDescription()).trim());
        protocol.setDisabled(dto.isDisabled());
        protocol.setCreatedBy(operatorId);
        protocol.setUpdatedBy(operatorId);
        normalizeVersion(protocol);
        this.insert(protocol);
        deviceEntityRedisCacheService.cacheProtocol(protocol);
        publishRefresh(protocol, "UPSERT");
        return protocol;
    }

    @Override
    public Device_protocol updateProtocol(DeviceProtocolDTO dto, String operatorId, String tenantId) {
        Device_protocol protocol = getProtocol(dto.getId(), tenantId);
        checkCodeUnique(protocol.getId(), tenantId, dto.getCode());
        protocol.setName(Strings.sNull(dto.getName()).trim());
        protocol.setCode(Strings.sNull(dto.getCode()).trim());
        protocol.setScriptType(dto.getScriptType());
        protocol.setScriptContent(dto.getScriptContent());
        protocol.setScriptVersion(Strings.sNull(dto.getScriptVersion()).trim());
        protocol.setDescription(Strings.sNull(dto.getDescription()).trim());
        protocol.setDisabled(dto.isDisabled());
        protocol.setUpdatedBy(operatorId);
        normalizeVersion(protocol);
        this.updateIgnoreNull(protocol);
        deviceEntityRedisCacheService.cacheProtocol(protocol);
        publishRefresh(protocol, "UPSERT");
        return protocol;
    }

    @Override
    public void deleteProtocol(String id, String tenantId) {
        Device_protocol protocol = getProtocol(id, tenantId);
        this.delete(protocol.getId());
        deviceEntityRedisCacheService.evictProtocol(tenantId, protocol.getId());
        publishRefresh(protocol, "DELETE");
    }

    @Override
    public Device_protocol getProtocol(String id, String tenantId) {
        Device_protocol cached = deviceEntityRedisCacheService.getProtocol(tenantId, id);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag())) {
            return cached;
        }
        Device_protocol protocol = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (protocol == null) throw new BaseException("设备协议不存在");
        deviceEntityRedisCacheService.cacheProtocol(protocol);
        return protocol;
    }

    @Override
    public List<Device_protocol> listEnabled(String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false).asc("createdAt"));
    }

    @Override
    public DeviceProtocolParseResultDTO debug(DeviceProtocolDebugRequestDTO request, String tenantId) {
        if (request == null) {
            throw new BaseException("调试请求不能为空");
        }
        try {
            if (deviceProtocolDebugProvider != null) {
                return deviceProtocolDebugProvider.debug(request);
            }
        } catch (RpcException e) {
            return deviceLocalScriptExecutionService.debug(request);
        }
        return deviceLocalScriptExecutionService.debug(request);
    }

    @Override
    public String encodeCommand(String protocolId, String commandCode, String payloadJson, Device_info device, String tenantId) {
        Device_protocol protocol = getProtocol(protocolId, tenantId);
        DeviceProtocolEncodeRequestDTO request = new DeviceProtocolEncodeRequestDTO();
        request.setScriptType(protocol.getScriptType());
        request.setScriptContent(protocol.getScriptContent());
        request.setCommandJson(buildCommandJson(commandCode, payloadJson, device));
        try {
            if (deviceProtocolDebugProvider != null) {
                return deviceProtocolDebugProvider.encode(request);
            }
        } catch (RpcException e) {
            return deviceLocalScriptExecutionService.encode(request);
        }
        return deviceLocalScriptExecutionService.encode(request);
    }

    private void checkCodeUnique(String id, String tenantId, String code) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("code", "=", Strings.sNull(code).trim()).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) cnd.and("id", "<>", id);
        if (this.count(cnd) > 0) throw new BaseException("协议编码已存在");
    }

    private void normalizeVersion(Device_protocol protocol) {
        protocol.setScriptVersion(Strings.sBlank(protocol.getScriptVersion(), String.valueOf(System.currentTimeMillis())).trim());
    }

    private String buildCommandJson(String commandCode, String payloadJson, Device_info device) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("direction", "D");
            map.put("cmd", Strings.sNull(commandCode).trim());
            map.put("method", Strings.sNull(commandCode).trim());
            if (device != null) {
                map.put("deviceId", Strings.sBlank(device.getId(), ""));
                map.put("deviceCode", Strings.sBlank(device.getDeviceCode(), ""));
                map.put("address", Strings.sBlank(device.getDeviceCode(), ""));
                map.put("imei", Strings.sBlank(device.getImei(), ""));
                map.put("iccid", Strings.sBlank(device.getIccid(), ""));
                map.put("productId", Strings.sBlank(device.getProductId(), ""));
                map.put("productKey", Strings.sBlank(device.getProductKey(), ""));
            }
            if (Strings.isNotBlank(payloadJson)) {
                Object payload = objectMapper.readValue(payloadJson, Object.class);
                if (payload instanceof java.util.Map<?, ?> payloadMap) {
                    payloadMap.forEach((key, value) -> map.put(String.valueOf(key), value));
                } else {
                    map.put("payload", payload);
                }
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new BaseException(Strings.sBlank(e.getMessage(), "构建设备指令JSON失败"));
        }
    }

    private void publishRefresh(Device_protocol protocol, String action) {
        try {
            DeviceScriptRefreshMessageDTO message = new DeviceScriptRefreshMessageDTO();
            message.setTenantId(protocol.getTenantId());
            message.setProtocolId(protocol.getId());
            message.setScriptVersion(protocol.getScriptVersion());
            message.setAction(action);
            stringRedisTemplate.convertAndSend(DeviceScriptChannels.SCRIPT_REFRESH, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            throw new BaseException(Strings.sBlank(e.getMessage(), "协议脚本缓存刷新消息发送失败"));
        }
    }
}
