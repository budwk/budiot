package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备规则条件DTO")
public class DeviceRuleConditionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String field;
    private String fieldName;
    private String dataType;
    private String operator;
    private String value;
}
