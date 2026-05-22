package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "规则联动参数DTO")
public class DeviceRuleLinkageParamDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String identifier;
    private String name;
    private String dataType;
    private String value;
}
