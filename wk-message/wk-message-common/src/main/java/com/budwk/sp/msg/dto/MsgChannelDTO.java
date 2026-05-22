package com.budwk.sp.msg.dto;

import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "消息渠道DTO")
public class MsgChannelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;

    private String tenantId;

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 120, message = "渠道名称长度不能超过120")
    private String name;

    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 80, message = "渠道编码长度不能超过80")
    private String code;

    @NotNull(message = "渠道类型不能为空")
    private MsgChannelType channelType;

    @NotNull(message = "提供商不能为空")
    private MsgProviderType providerType;

    @NotBlank(message = "渠道配置不能为空")
    private String configJson;

    private boolean defaultFlag;

    private boolean disabled;
}
