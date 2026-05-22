package com.budwk.sp.msg.entity;

import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("msg_channel")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_MSG_CHANNEL_CODE", fields = {"tenantId", "code"}, unique = true),
        @Index(name = "INDEX_MSG_CHANNEL_PROVIDER", fields = {"tenantId", "providerType"}, unique = false)
})
public class Msg_channel extends BaseEntity implements Serializable {
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
    @Comment("渠道名称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;

    @Column
    @Comment("渠道编码")
    @ColDefine(type = ColType.VARCHAR, width = 80)
    private String code;

    @Column
    @Comment("渠道类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgChannelType channelType;

    @Column
    @Comment("提供商类型")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    private MsgProviderType providerType;

    @Column
    @Comment("渠道配置JSON")
    @ColDefine(type = ColType.TEXT)
    private String configJson;

    @Column
    @Comment("默认渠道")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean defaultFlag;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
}
