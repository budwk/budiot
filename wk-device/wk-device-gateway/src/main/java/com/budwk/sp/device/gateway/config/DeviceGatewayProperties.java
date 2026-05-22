package com.budwk.sp.device.gateway.config;

import com.budwk.sp.device.network.DeviceGatewayBinding;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component("deviceGatewayProperties")
@ConfigurationProperties(prefix = "wk.device.gateway")
public class DeviceGatewayProperties {
    private String nodeId = "gateway-node-01";
    private String host = "127.0.0.1";
    private int heartbeatSeconds = 10;
    private int syncSeconds = 60;
    private String downlinkTopic = "wk.device.downlink.command";
    private String broadcastTopic = "wk.device.gateway.broadcast";
}
