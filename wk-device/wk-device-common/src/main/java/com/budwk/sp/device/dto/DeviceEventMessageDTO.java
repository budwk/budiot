package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备事件告警消息")
public class DeviceEventMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventCode;
    private String eventName;
    private String level;
    private String sourceType;
    private String contentJson;
    private Long deviceAt;
}
