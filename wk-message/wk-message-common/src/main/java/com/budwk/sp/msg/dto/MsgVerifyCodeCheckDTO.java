package com.budwk.sp.msg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "验证码校验DTO")
public class MsgVerifyCodeCheckDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;

    @NotBlank(message = "接收地址不能为空")
    private String receiver;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
