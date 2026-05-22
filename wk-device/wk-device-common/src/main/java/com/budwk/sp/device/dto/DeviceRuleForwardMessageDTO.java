package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "规则转发消息")
public class DeviceRuleForwardMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;
    private String productId;
    private String productKey;
    private String deviceId;
    private String deviceCode;
    private String sourceScene;
    private String sourceIdentifier;
    private String ruleCode;
    private String ruleName;
    private String targetType;
    private String targetTopic;
    private String contentJson;
    private Long triggerAt;
}
