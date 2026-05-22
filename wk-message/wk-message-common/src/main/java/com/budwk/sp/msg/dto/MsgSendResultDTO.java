package com.budwk.sp.msg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "发送结果DTO")
public class MsgSendResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String receiver;
    private String requestId;
    private String code;
    private String message;
    private Long sendAt;
}
