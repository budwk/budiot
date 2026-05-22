package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceGatewayNodeDTO;
import com.budwk.sp.device.dto.DeviceGatewayDTO;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.services.DeviceGatewayService;
import com.budwk.sp.device.services.DeviceProtocolService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeviceGatewayServiceImpl extends BaseServiceImpl<Device_gateway> implements DeviceGatewayService {
    private final DeviceProtocolService deviceProtocolService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceMessageTemplate deviceMessageTemplate;

    public DeviceGatewayServiceImpl(Dao dao, DeviceProtocolService deviceProtocolService, StringRedisTemplate stringRedisTemplate, DeviceMessageTemplate deviceMessageTemplate) {
        super(dao);
        this.deviceProtocolService = deviceProtocolService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.deviceMessageTemplate = deviceMessageTemplate;
    }

    @Override
    public Device_gateway createGateway(DeviceGatewayDTO dto, String operatorId, String tenantId) {
        Device_gateway gateway = new Device_gateway();
        build(dto, gateway, operatorId, tenantId);
        gateway.setRuntimeStatus(DeviceGatewayStatus.STOPPED);
        gateway.setCreatedBy(operatorId);
        this.insert(gateway);
        return gateway;
    }

    @Override
    public Device_gateway updateGateway(DeviceGatewayDTO dto, String operatorId, String tenantId) {
        Device_gateway gateway = getGateway(dto.getId(), tenantId);
        DeviceGatewayStatus oldStatus = gateway.getRuntimeStatus();
        build(dto, gateway, operatorId, tenantId);
        this.updateIgnoreNull(gateway);
        
        // 如果网关正在运行且配置有变化，发送配置更新通知
        if (oldStatus == DeviceGatewayStatus.RUNNING) {
            publishConfigUpdate(gateway, operatorId);
        }
        
        return gateway;
    }

    @Override
    public void deleteGateway(String id, String tenantId) {
        Device_gateway gateway = getGateway(id, tenantId);
        gateway.setRuntimeStatus(DeviceGatewayStatus.STOPPED);
        this.updateIgnoreNull(gateway);
        this.delete(gateway.getId());
        publishRefresh(gateway, "DELETE", "system");
    }

    @Override
    public Device_gateway getGateway(String id, String tenantId) {
        Device_gateway gateway = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (gateway == null) {
            throw new BaseException("设备网关不存在");
        }
        enrichRuntime(gateway);
        if (Strings.isNotBlank(gateway.getProtocolId())) {
            gateway.setProtocol(deviceProtocolService.getProtocol(gateway.getProtocolId(), tenantId));
        }
        return gateway;
    }

    @Override
    public Device_gateway changeStatus(String id, String tenantId, DeviceGatewayStatus status, String operatorId) {
        Device_gateway gateway = getGateway(id, tenantId);
        gateway.setRuntimeStatus(status);
        gateway.setUpdatedBy(operatorId);
        if (status == DeviceGatewayStatus.RUNNING) {
            gateway.setLastStartedAt(System.currentTimeMillis());
            gateway.setLastError("");
        }
        this.updateIgnoreNull(gateway);
        publishControl(gateway, status, operatorId);
        return gateway;
    }

    @Override
    public List<Device_gateway> listEnabled(String tenantId, DeviceNetworkProtocol protocol) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false);
        if (protocol != null) {
            cnd.and("networkProtocol", "=", protocol);
        }
        List<Device_gateway> gateways = this.query(cnd.asc("createdAt"));
        gateways.forEach(this::enrichRuntime);
        return gateways;
    }

    @Override
    public Map<String, Device_gateway> getGatewayMap(List<String> ids, String tenantId) {
        Map<String, Device_gateway> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        List<Device_gateway> gateways = this.query(Cnd.where("tenantId", "=", tenantId).and("id", "in", ids).and("delFlag", "=", false));
        for (Device_gateway gateway : gateways) {
            enrichRuntime(gateway);
            map.put(gateway.getId(), gateway);
        }
        return map;
    }

    @Override
    public List<DeviceGatewayNodeDTO> listGatewayNodes(String tenantId, DeviceNetworkProtocol protocol) {
        return listEnabled(tenantId, protocol).stream().map(this::toNode).toList();
    }

    private Device_gateway build(DeviceGatewayDTO dto, Device_gateway gateway, String operatorId, String tenantId) {
        validate(dto, tenantId, gateway.getId());
        gateway.setTenantId(tenantId);
        gateway.setName(Strings.sNull(dto.getName()).trim());
        gateway.setNetworkProtocol(dto.getNetworkProtocol());
        gateway.setGatewayMode(dto.getGatewayMode());
        gateway.setProtocolId(Strings.sNull(dto.getProtocolId()).trim());
        gateway.setHost(Strings.sBlank(dto.getHost(), "0.0.0.0"));
        gateway.setPort(dto.getPort());
        gateway.setPath(Strings.sBlank(dto.getPath(), "/"));
        gateway.setRemoteHost(Strings.sNull(dto.getRemoteHost()).trim());
        gateway.setRemotePort(dto.getRemotePort());
        gateway.setClientId(Strings.sNull(dto.getClientId()).trim());
        gateway.setUsername(Strings.sNull(dto.getUsername()).trim());
        gateway.setPassword(Strings.sNull(dto.getPassword()).trim());
        gateway.setSubscribeTopic(Strings.sNull(dto.getSubscribeTopic()).trim());
        gateway.setPublishTopic(Strings.sNull(dto.getPublishTopic()).trim());
        gateway.setAllowAnonymous(dto.isAllowAnonymous());
        gateway.setAutoStart(dto.isAutoStart());
        gateway.setDisabled(dto.isDisabled());
        gateway.setDescription(Strings.sNull(dto.getDescription()).trim());
        gateway.setUpdatedBy(operatorId);
        if (gateway.getRuntimeStatus() == null) {
            gateway.setRuntimeStatus(dto.getRuntimeStatus() == null ? DeviceGatewayStatus.STOPPED : dto.getRuntimeStatus());
        }
        return gateway;
    }

    private void validate(DeviceGatewayDTO dto, String tenantId, String currentId) {
        if (dto.getNetworkProtocol() == null || dto.getGatewayMode() == null) {
            throw new BaseException("网关协议或模式不能为空");
        }
        if (!isModeCompatible(dto.getNetworkProtocol(), dto.getGatewayMode())) {
            throw new BaseException("网关模式与网络协议不匹配");
        }
        if (requiresLocalEndpoint(dto.getGatewayMode()) && (dto.getPort() == null || dto.getPort() <= 0)) {
            throw new BaseException("监听端口不能为空");
        }
        if (dto.getGatewayMode() == DeviceGatewayMode.MQTT_CLIENT) {
            if (Strings.isBlank(dto.getRemoteHost()) || dto.getRemotePort() == null || dto.getRemotePort() <= 0) {
                throw new BaseException("MQTT客户端模式需要填写远程地址和端口");
            }
        }
        if (dto.getGatewayMode() == DeviceGatewayMode.HTTP_SERVER && Strings.isBlank(dto.getPath())) {
            throw new BaseException("HTTP服务端模式需要填写访问路径");
        }
        validatePortConflict(dto, tenantId, currentId);
        deviceProtocolService.getProtocol(dto.getProtocolId(), tenantId);
    }

    private void validatePortConflict(DeviceGatewayDTO dto, String tenantId, String currentId) {
        if (!requiresLocalEndpoint(dto.getGatewayMode()) || dto.getPort() == null || dto.getPort() <= 0) {
            return;
        }
        List<Device_gateway> gateways = this.query(Cnd.where("delFlag", "=", false).and("port", "=", dto.getPort()));
        for (Device_gateway gateway : gateways) {
            if (Strings.isNotBlank(currentId) && currentId.equals(gateway.getId())) {
                continue;
            }
            if (!hostConflict(dto.getHost(), gateway.getHost())) {
                continue;
            }
            throw new BaseException("端口已被网关【" + gateway.getName() + "】占用，请更换端口或停止冲突网关");
        }
    }

    private boolean hostConflict(String hostA, String hostB) {
        String a = normalizeHost(hostA);
        String b = normalizeHost(hostB);
        return isWildcardHost(a) || isWildcardHost(b) || a.equalsIgnoreCase(b);
    }

    private String normalizeHost(String host) {
        return Strings.sBlank(host, "0.0.0.0").trim();
    }

    private boolean isWildcardHost(String host) {
        return "0.0.0.0".equals(host) || "::".equals(host) || "*".equals(host);
    }

    private boolean requiresLocalEndpoint(DeviceGatewayMode mode) {
        return mode != DeviceGatewayMode.MQTT_CLIENT;
    }

    private boolean isModeCompatible(DeviceNetworkProtocol protocol, DeviceGatewayMode mode) {
        return switch (protocol) {
            case MQTT -> mode == DeviceGatewayMode.MQTT_SERVER || mode == DeviceGatewayMode.MQTT_CLIENT;
            case TCP -> mode == DeviceGatewayMode.TCP_SERVER;
            case UDP -> mode == DeviceGatewayMode.UDP_SERVER;
            case HTTP -> mode == DeviceGatewayMode.HTTP_SERVER;
            case MODBUS_TCP -> mode == DeviceGatewayMode.MODBUS_TCP_SERVER;
        };
    }

    private DeviceGatewayNodeDTO toNode(Device_gateway gateway) {
        DeviceGatewayNodeDTO dto = new DeviceGatewayNodeDTO();
        dto.setGatewayId(gateway.getId());
        dto.setNodeId(gateway.getId());
        dto.setName(gateway.getName());
        dto.setProtocol(gateway.getNetworkProtocol() == null ? "" : gateway.getNetworkProtocol().getValue());
        dto.setGatewayMode(gateway.getGatewayMode() == null ? "" : gateway.getGatewayMode().getValue());
        dto.setProtocolId(gateway.getProtocolId());
        dto.setProtocolName(gateway.getProtocol() == null ? "" : gateway.getProtocol().getName());
        dto.setHost(gateway.getHost());
        dto.setPort(gateway.getPort());
        dto.setPath(gateway.getPath());
        dto.setRemoteHost(gateway.getRemoteHost());
        dto.setRemotePort(gateway.getRemotePort());
        dto.setStatus(gateway.getRuntimeStatus() == null ? "" : gateway.getRuntimeStatus().getValue());
        dto.setLastSeenAt(gateway.getLastSeenAt());
        return dto;
    }

    private void enrichRuntime(Device_gateway gateway) {
        Map<Object, Object> runtime = stringRedisTemplate.opsForHash().entries(runtimeKey(gateway.getTenantId(), gateway.getId()));
        if (!runtime.isEmpty()) {
            gateway.setLastSeenAt(parseLong(runtime.get("lastSeenAt")));
            if (runtime.containsKey("status")) {
                try {
                    gateway.setRuntimeStatus(DeviceGatewayStatus.valueOf(String.valueOf(runtime.get("status"))));
                } catch (Exception ignored) {
                }
            }
            if (runtime.containsKey("lastError")) {
                gateway.setLastError(String.valueOf(runtime.get("lastError")));
            }
        }
        if (Strings.isNotBlank(gateway.getProtocolId())) {
            try {
                gateway.setProtocol(deviceProtocolService.getProtocol(gateway.getProtocolId(), gateway.getTenantId()));
            } catch (Exception ignored) {
            }
        }
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String runtimeKey(String tenantId, String gatewayId) {
        return "wk:device:gateway:runtime:" + tenantId + ":" + gatewayId;
    }

    private void publishControl(Device_gateway gateway, DeviceGatewayStatus status, String operatorId) {
        DeviceGatewayControlMessageDTO payload = new DeviceGatewayControlMessageDTO();
        payload.setGatewayId(gateway.getId());
        payload.setGatewayName(gateway.getName());
        payload.setTenantId(gateway.getTenantId());
        payload.setTargetStatus(status);
        payload.setOperatorId(operatorId);
        DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(gateway.getTenantId());
        envelope.setGatewayNodeId(gateway.getId());
        envelope.setScene(DeviceMessageScene.GATEWAY_CONTROL);
        envelope.setPattern(DeviceMessagePattern.BROADCAST);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.getHeaders().put("gatewayId", gateway.getId());
        envelope.getHeaders().put("targetStatus", status.getValue());
        envelope.setPayload(payload);
        deviceMessageTemplate.publish(envelope, gateway.getId(), operatorId);
    }
    
    /**
     * 发布配置更新通知
     */
    private void publishConfigUpdate(Device_gateway gateway, String operatorId) {
        publishRefresh(gateway, "CONFIG_UPDATE", operatorId);
    }

    private void publishRefresh(Device_gateway gateway, String action, String operatorId) {
        DeviceGatewayControlMessageDTO payload = new DeviceGatewayControlMessageDTO();
        payload.setGatewayId(gateway.getId());
        payload.setGatewayName(gateway.getName());
        payload.setTenantId(gateway.getTenantId());
        payload.setAction(action);
        payload.setOperatorId(operatorId);
        DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(gateway.getTenantId());
        envelope.setGatewayNodeId(gateway.getId());
        envelope.setScene(DeviceMessageScene.GATEWAY_CONTROL);
        envelope.setPattern(DeviceMessagePattern.BROADCAST);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.getHeaders().put("gatewayId", gateway.getId());
        envelope.getHeaders().put("action", action);
        envelope.setPayload(payload);
        deviceMessageTemplate.publish(envelope, gateway.getId(), operatorId);

        log.info("网关刷新通知已发送: gatewayId={}, action={}, operator={}", gateway.getId(), action, operatorId);
    }
}
