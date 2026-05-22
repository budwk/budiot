package com.budwk.sp.msg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "验证码发送DTO")
public class MsgVerifyCodeSendDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String channelId;
    private String templateId;
    private String bizScene;

    @NotBlank(message = "接收地址不能为空")
    private String receiver;

    private String loginname;
    private String subject;
    private Integer length = 6;
}
