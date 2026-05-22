package com.budwk.sp.device.network;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DeviceNetworkInboundMessage {
    private String bindingId;
    private String nodeId;
    private String tenantId;
    private String productKey;
    private String deviceCode;
    private String sessionId;
    private DeviceNetworkProtocol protocol;
    private String remoteAddress;
    private String endpoint;
    private String payload;
    private Long occurredAt;
    private Map<String, String> headers = new HashMap<>();
}
