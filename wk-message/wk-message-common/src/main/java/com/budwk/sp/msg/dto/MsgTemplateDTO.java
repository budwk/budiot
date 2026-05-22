package com.budwk.sp.msg.dto;

import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "消息模板DTO")
public class MsgTemplateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;

    private String tenantId;

    @NotBlank(message = "渠道不能为空")
    private String channelId;

    @NotNull(message = "渠道类型不能为空")
    private MsgChannelType channelType;

    @NotNull(message = "提供商不能为空")
    private MsgProviderType providerType;

    @NotNull(message = "模板业务类型不能为空")
    private MsgTemplateBizType bizType;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 120, message = "模板名称长度不能超过120")
    private String name;

    @Size(max = 120, message = "模板代码长度不能超过120")
    private String templateCode;

    private String content;

    private String paramsJson;

    private boolean disabled;
}
