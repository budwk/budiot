package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceRuleTargetType;
import com.budwk.sp.device.enums.DeviceRuleTriggerScene;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_rule")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_RULE_CODE", fields = {"tenantId", "code"}, unique = true),
        @Index(name = "INDEX_DEVICE_RULE_TRIGGER", fields = {"tenantId", "triggerScene"}, unique = false)
})
public class Device_rule extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Name
    @Column
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @Comment("规则名称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;

    @Column
    @Comment("规则编码")
    @ColDefine(type = ColType.VARCHAR, width = 80)
    private String code;

    @Column
    @Comment("触发场景")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private DeviceRuleTriggerScene triggerScene;

    @Column
    @Comment("来源产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String sourceProductId;

    @Column
    @Comment("来源设备ID，为空表示全部设备")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String sourceDeviceId;

    @Column
    @Comment("触发标识")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String triggerIdentifier;

    @Column
    @Comment("条件JSON")
    @ColDefine(type = ColType.TEXT)
    private String conditionJson;

    @Column
    @Comment("目标类型")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private DeviceRuleTargetType targetType;

    @Column
    @Comment("动作标题")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String actionTitle;

    @Column
    @Comment("动作内容")
    @ColDefine(type = ColType.TEXT)
    private String actionContent;

    @Column
    @Comment("短信渠道ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String messageChannelId;

    @Column
    @Comment("通知用户JSON")
    @ColDefine(type = ColType.TEXT)
    private String notifyUserIdsJson;

    @Column
    @Comment("HTTP目标地址")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String targetUrl;

    @Column
    @Comment("队列目标主题")
    @ColDefine(type = ColType.VARCHAR, width = 160)
    private String targetTopic;

    @Column
    @Comment("联动目标产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String linkageProductId;

    @Column
    @Comment("联动目标设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String linkageDeviceId;

    @Column
    @Comment("联动服务标识")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String linkageServiceIdentifier;

    @Column
    @Comment("联动参数JSON")
    @ColDefine(type = ColType.TEXT)
    private String linkageParamsJson;

    @Column
    @Comment("说明")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
}
