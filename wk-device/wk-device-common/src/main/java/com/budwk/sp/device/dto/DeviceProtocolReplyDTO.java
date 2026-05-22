package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议解析后生成的回复指令")
public class DeviceProtocolReplyDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String commandCode;
    private String payload;
    private String payloadJson;
    private boolean replyRequired;
    private Long deadlineAt;
}
