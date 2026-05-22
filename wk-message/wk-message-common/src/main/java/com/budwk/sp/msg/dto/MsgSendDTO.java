package com.budwk.sp.msg.dto;

import com.budwk.sp.msg.enums.MsgTemplateBizType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "发送消息DTO")
public class MsgSendDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;

    @NotBlank(message = "渠道ID不能为空")
    private String channelId;

    private String templateId;

    private MsgTemplateBizType bizType;

    private String title;

    private String content;

    private String paramsJson;

    private String[] userIds;

    private String[] receivers;
}
