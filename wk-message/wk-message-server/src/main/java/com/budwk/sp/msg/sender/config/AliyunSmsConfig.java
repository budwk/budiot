package com.budwk.sp.msg.sender.config;

import lombok.Data;

@Data
public class AliyunSmsConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId;
    private String endpoint;
    private String signName;
}
