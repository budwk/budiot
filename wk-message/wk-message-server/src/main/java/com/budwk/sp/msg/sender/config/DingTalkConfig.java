package com.budwk.sp.msg.sender.config;

import lombok.Data;

@Data
public class DingTalkConfig {
    private String webhookUrl;
    private String secret;
}
