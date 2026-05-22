package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议解析后的事件数据")
public class DeviceParsedEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventCode;
    private String eventName;
    private String level;
    private String sourceType;
    private String contentJson;
    private Long deviceAt;
}
