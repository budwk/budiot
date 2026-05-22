package com.budwk.sp.device.message.config;

import com.budwk.sp.device.enums.DeviceMessageProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wk.device.message")
public class DeviceMessageProperties {
    private boolean enabled = true;
    private DeviceMessageProviderType provider = DeviceMessageProviderType.REDIS;
    private String topicPrefix = "wk.device";
    private long sendTimeout = 3000L;
    private String redisStreamPrefix = "wk:device:stream:";
    private String redisChannelPrefix = "wk:device:channel:";
    private int redisPollCount = 20;
    private long redisBlockMs = 1000L;
}
