package com.budwk.sp.device.network;

import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import lombok.Data;

@Data
public class DeviceGatewayBinding {
    private String gatewayId;
    private String bindingId;
    private String nodeId;
    private String tenantId;
    private String host = "0.0.0.0";
    private int port;
    private String path = "/";
    private String productKey;
    private DeviceNetworkProtocol protocol;
    private DeviceGatewayMode gatewayMode;
    private String protocolId;
    private String remoteHost;
    private Integer remotePort;
    private String clientId;
    private String subscribeTopic;
    private String publishTopic;
    private boolean allowAnonymous = true;
    private String username;
    private String password;
    private String clientIdPrefix;
    private String uplinkTopicPrefix = "up/";
    private String downlinkTopicPrefix = "cmd/";
    private boolean enabled = true;
}
