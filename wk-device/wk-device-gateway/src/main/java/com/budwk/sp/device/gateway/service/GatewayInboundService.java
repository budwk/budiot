package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.dto.DeviceAccessDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveResultDTO;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.budwk.sp.device.gateway.support.GatewayDeviceAccessService;
import com.budwk.sp.device.gateway.support.GatewayProtocolCacheService;
import com.budwk.sp.device.gateway.support.GatewayProtocolIdentityService;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.network.DeviceNetworkHandler;
import com.budwk.sp.device.network.DeviceNetworkInboundMessage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GatewayInboundService implements DeviceNetworkHandler {
    private final DeviceMessageTemplate deviceMessageTemplate;
    private final GatewaySessionService gatewaySessionService;
    private final GatewayDeviceAccessService gatewayDeviceAccessService;
    private final GatewayProtocolCacheService gatewayProtocolCacheService;
    private final GatewayProtocolIdentityService gatewayProtocolIdentityService;

    public GatewayInboundService(DeviceMessageTemplate deviceMessageTemplate,
                                 GatewaySessionService gatewaySessionService,
                                 GatewayDeviceAccessService gatewayDeviceAccessService,
                                 GatewayProtocolCacheService gatewayProtocolCacheService,
                                 GatewayProtocolIdentityService gatewayProtocolIdentityService) {
        this.deviceMessageTemplate = deviceMessageTemplate;
        this.gatewaySessionService = gatewaySessionService;
        this.gatewayDeviceAccessService = gatewayDeviceAccessService;
        this.gatewayProtocolCacheService = gatewayProtocolCacheService;
        this.gatewayProtocolIdentityService = gatewayProtocolIdentityService;
    }

    @Override
    public void onMessage(DeviceNetworkInboundMessage message) {
        String tenantId = Strings.sBlank(message.getTenantId(), "public");
        DeviceProtocolIdentityResolveResultDTO resolvedIdentity = resolveIdentity(message);
        String productKey = Strings.sBlank(
                resolvedIdentity == null ? "" : resolvedIdentity.getProductKey(),
                Strings.sNull(message.getProductKey()).trim()
        );
        String identityType = Strings.sBlank(
                resolvedIdentity == null ? "" : resolvedIdentity.getIdentityType(),
                "DEVICE_CODE"
        );
        String identityValue = Strings.sBlank(
                resolvedIdentity == null ? "" : resolvedIdentity.getIdentityValue(),
                Strings.sNull(message.getDeviceCode()).trim()
        );
        if (Strings.isBlank(identityValue)) {
            log.warn("忽略空设备号上报: gatewayNodeId={}, endpoint={}", message.getNodeId(), message.getEndpoint());
            return;
        }
        DeviceAccessDTO access = gatewayDeviceAccessService.getAccessByIdentity(tenantId, identityType, identityValue, productKey);
        if (access == null) {
            log.warn("未匹配到设备接入信息，忽略上报: tenantId={}, productKey={}, identityType={}, identityValue={}, gatewayNodeId={}",
                    tenantId, productKey, identityType, identityValue, message.getNodeId());
            return;
        }
        if (access.isDisabled()) {
            log.warn("设备已禁用，忽略上报: tenantId={}, productKey={}, deviceCode={}",
                    tenantId, access.getProductKey(), access.getDeviceCode());
            return;
        }
        gatewaySessionService.bind(access, message);
        log.info("网关接收上报成功: tenantId={}, productKey={}, deviceCode={}, gatewayNodeId={}, protocol={}, payloadLength={}",
                access.getTenantId(), access.getProductKey(), access.getDeviceCode(), message.getNodeId(),
                message.getProtocol() == null ? "" : message.getProtocol().getValue(),
                Strings.sNull(message.getPayload()).length());

        DeviceRawMessageDTO payload = new DeviceRawMessageDTO();
        payload.setProtocol(message.getProtocol().getValue());
        payload.setEndpoint(Strings.sBlank(message.getEndpoint(), ""));
        payload.setSourceIp(Strings.sBlank(message.getRemoteAddress(), ""));
        payload.setPayload(Strings.sBlank(message.getPayload(), ""));
        payload.setReceivedAt(message.getOccurredAt());

        DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(access.getTenantId());
        envelope.setProductId(access.getProductId());
        envelope.setProductKey(access.getProductKey());
        envelope.setDeviceId(access.getId());
        envelope.setDeviceCode(access.getDeviceCode());
        envelope.setGatewayNodeId(message.getNodeId());
        envelope.setScene(DeviceMessageScene.RAW_UPLINK);
        envelope.setPattern(DeviceMessagePattern.TOPIC);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.setRoutingKey(access.getDeviceCode());
        envelope.getHeaders().putAll(message.getHeaders());
        envelope.getHeaders().put("bindingId", message.getBindingId());
        envelope.getHeaders().put("sessionId", Strings.sBlank(message.getSessionId(), ""));
        envelope.getHeaders().put("identityType", identityType);
        envelope.getHeaders().put("identityValue", identityValue);
        envelope.setPayload(payload);
        deviceMessageTemplate.publish(envelope, access.getId(), "gateway");
    }

    private DeviceProtocolIdentityResolveResultDTO resolveIdentity(DeviceNetworkInboundMessage message) {
        String protocolId = Strings.sNull(message.getHeaders().get("protocolId")).trim();
        if (Strings.isBlank(protocolId)) {
            return null;
        }
        try {
            Device_protocol protocol = gatewayProtocolCacheService.getProtocol(Strings.sBlank(message.getTenantId(), "public"), protocolId);
            if (protocol == null || protocol.isDisabled()) {
                return null;
            }
            DeviceProtocolIdentityResolveRequestDTO request = new DeviceProtocolIdentityResolveRequestDTO();
            request.setTenantId(Strings.sBlank(message.getTenantId(), "public"));
            request.setProtocolId(protocolId);
            request.setProductKey(Strings.sNull(message.getProductKey()).trim());
            request.setDeviceCode(Strings.sNull(message.getDeviceCode()).trim());
            request.setNetworkProtocol(message.getProtocol() == null ? "" : message.getProtocol().getValue());
            request.setEndpoint(Strings.sBlank(message.getEndpoint(), ""));
            request.setSourceIp(Strings.sBlank(message.getRemoteAddress(), ""));
            request.setPayload(Strings.sBlank(message.getPayload(), ""));
            request.setReceivedAt(message.getOccurredAt());
            request.getHeaders().putAll(message.getHeaders());
            java.util.Map<String, Object> input = new java.util.LinkedHashMap<>();
            input.put("tenantId", request.getTenantId());
            input.put("protocolId", request.getProtocolId());
            input.put("productKey", request.getProductKey());
            input.put("deviceCode", request.getDeviceCode());
            input.put("networkProtocol", request.getNetworkProtocol());
            input.put("endpoint", request.getEndpoint());
            input.put("sourceIp", request.getSourceIp());
            input.put("payload", request.getPayload());
            input.put("receivedAt", request.getReceivedAt());
            input.put("headers", request.getHeaders());
            return gatewayProtocolIdentityService.resolveIdentity(protocol, input);
        } catch (Exception e) {
            log.warn("协议身份预解析失败，回退默认设备识别: protocolId={}, gatewayNodeId={}, error={}",
                    protocolId, message.getNodeId(), Strings.sBlank(e.getMessage(), e.getClass().getSimpleName()));
            return null;
        }
    }
}
