package com.budwk.sp.device.handler.providers;

import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolEncodeRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveResultDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.handler.service.DeviceProtocolScriptCacheService;
import com.budwk.sp.device.handler.service.DeviceScriptExecutionService;
import com.budwk.sp.device.providers.IDeviceProtocolDebugProvider;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.nutz.lang.Strings;

import java.util.Map;

@DubboService(interfaceClass = IDeviceProtocolDebugProvider.class)
public class DeviceProtocolDebugProvider implements IDeviceProtocolDebugProvider {
    private final DeviceScriptExecutionService deviceScriptExecutionService;
    private final DeviceProtocolScriptCacheService deviceProtocolScriptCacheService;
    private final ObjectMapper objectMapper;

    public DeviceProtocolDebugProvider(DeviceScriptExecutionService deviceScriptExecutionService,
                                       DeviceProtocolScriptCacheService deviceProtocolScriptCacheService,
                                       ObjectMapper objectMapper) {
        this.deviceScriptExecutionService = deviceScriptExecutionService;
        this.deviceProtocolScriptCacheService = deviceProtocolScriptCacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceProtocolParseResultDTO debug(DeviceProtocolDebugRequestDTO request) {
        if (request == null || Strings.isBlank(request.getInputJson())) {
            throw new BaseException("调试输入不能为空");
        }
        try {
            Map<String, Object> input = objectMapper.readValue(request.getInputJson(), new TypeReference<>() {});
            return deviceScriptExecutionService.debug(request, input);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(Strings.sBlank(e.getMessage(), "调试输入JSON格式不正确"));
        }
    }

    @Override
    public String encode(DeviceProtocolEncodeRequestDTO request) {
        if (request == null || Strings.isBlank(request.getCommandJson())) {
            throw new BaseException("指令JSON不能为空");
        }
        return deviceScriptExecutionService.encode(request);
    }

    @Override
    public DeviceProtocolIdentityResolveResultDTO resolveIdentity(DeviceProtocolIdentityResolveRequestDTO request) {
        if (request == null || Strings.isBlank(request.getTenantId()) || Strings.isBlank(request.getProtocolId())) {
            throw new BaseException("租户ID和协议ID不能为空");
        }
        Device_protocol protocol = deviceProtocolScriptCacheService.getProtocol(request.getTenantId(), request.getProtocolId());
        if (protocol == null || protocol.isDisabled()) {
            throw new BaseException("设备协议不存在或已禁用");
        }
        try {
            Map<String, Object> input = objectMapper.convertValue(request, new TypeReference<>() {});
            return deviceScriptExecutionService.resolveIdentity(protocol, input);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(Strings.sBlank(e.getMessage(), "身份预解析失败"));
        }
    }
}
