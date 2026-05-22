package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

@Service
public class GatewayControlService {
    private final GatewayNetworkManager gatewayNetworkManager;

    public GatewayControlService(GatewayNetworkManager gatewayNetworkManager) {
        this.gatewayNetworkManager = gatewayNetworkManager;
    }

    public void handle(DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            return;
        }
        try {
            gatewayNetworkManager.refreshGateway(envelope.getPayload().getGatewayId());
        } catch (Exception e) {
            throw new IllegalStateException(Strings.sBlank(e.getMessage(), "网关运行时同步失败"), e);
        }
    }
}
