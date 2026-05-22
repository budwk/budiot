package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议解析后的属性数据")
public class DeviceParsedPropertyDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String identifier;
    private String name;
    private String valueJson;
    private String unit;
    private Long deviceAt;
}
