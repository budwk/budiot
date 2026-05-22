package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备原始通信报文")
public class DeviceRawMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String protocol;
    private String endpoint;
    private String sourceIp;
    private String payload;
    private String parsedJson;
    private Long receivedAt;
}
