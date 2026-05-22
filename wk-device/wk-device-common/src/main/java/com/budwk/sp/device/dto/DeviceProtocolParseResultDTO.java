package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "协议解析结果")
public class DeviceProtocolParseResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String messageType;
    private Long deviceAt;
    private String metadataJson;
    private List<DeviceParsedPropertyDTO> properties = new ArrayList<>();
    private List<DeviceParsedEventDTO> events = new ArrayList<>();
    private List<DeviceProtocolReplyDTO> replies = new ArrayList<>();
}
