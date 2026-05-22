package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备标准化上行消息")
public class DeviceUplinkMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String identifier;
    private String messageType;
    private String dataJson;
    private Long deviceAt;
    private String metadataJson;
}
