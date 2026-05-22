package com.budwk.sp.device.rule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("deviceRuleProperties")
@ConfigurationProperties(prefix = "wk.device.rule")
public class DeviceRuleProperties {
    private String normalizedTopic = "wk.device.uplink.normalized";
    private String eventTopic = "wk.device.event";
    private long webhookTimeoutMs = 3000L;
}
