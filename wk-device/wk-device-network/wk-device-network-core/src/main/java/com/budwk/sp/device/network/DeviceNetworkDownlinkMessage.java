package com.budwk.sp.device.network;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import lombok.Data;

@Data
public class DeviceNetworkDownlinkMessage {
    private String bindingId;
    private String gatewayNodeId;
    private String deviceId;
    private String deviceCode;
    private String sessionId;
    private DeviceNetworkProtocol protocol;
    private String topic;
    private String payload;
}
