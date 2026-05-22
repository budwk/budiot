package com.budwk.sp.device.handler.config;

import com.budwk.sp.device.support.DeviceScriptChannels;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("deviceHandlerProperties")
@ConfigurationProperties(prefix = "wk.device.handler")
public class DeviceHandlerProperties {
    private String rawTopic = "wk.device.uplink.raw";
    private long scriptTimeoutMs = 1500L;
    private String scriptRefreshChannel = DeviceScriptChannels.SCRIPT_REFRESH;
}
