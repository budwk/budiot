package com.budwk.sp.msg.sender.config;

import lombok.Data;

@Data
public class TencentSmsConfig {
    private String secretId;
    private String secretKey;
    private String sdkAppId;
    private String region;
    private String endpoint;
    private String signName;
}
