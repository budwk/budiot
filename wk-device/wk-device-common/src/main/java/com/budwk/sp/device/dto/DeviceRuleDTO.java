package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceRuleTargetType;
import com.budwk.sp.device.enums.DeviceRuleTriggerScene;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备规则DTO")
public class DeviceRuleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 120, message = "规则名称长度不能超过120")
    private String name;

    @NotBlank(message = "规则编码不能为空")
    @Size(max = 80, message = "规则编码长度不能超过80")
    private String code;

    @NotNull(message = "触发场景不能为空")
    private DeviceRuleTriggerScene triggerScene;

    @NotBlank(message = "来源产品不能为空")
    private String sourceProductId;

    private String sourceDeviceId;
    private String triggerIdentifier;

    private String conditionJson;

    @NotNull(message = "目标类型不能为空")
    private DeviceRuleTargetType targetType;

    @Size(max = 120, message = "动作标题长度不能超过120")
    private String actionTitle;

    private String actionContent;

    private String messageChannelId;

    private String notifyUserIdsJson;

    @Size(max = 255, message = "目标地址长度不能超过255")
    private String targetUrl;

    @Size(max = 160, message = "目标主题长度不能超过160")
    private String targetTopic;

    private String linkageProductId;

    private String linkageDeviceId;

    @Size(max = 120, message = "联动服务标识长度不能超过120")
    private String linkageServiceIdentifier;

    private String linkageParamsJson;

    @Size(max = 255, message = "说明长度不能超过255")
    private String description;

    private boolean disabled;
}
