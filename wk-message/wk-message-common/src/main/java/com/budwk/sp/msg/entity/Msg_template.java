package com.budwk.sp.msg.entity;

import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("msg_template")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_MSG_TEMPLATE_BIZ", fields = {"tenantId", "bizType", "channelId"}, unique = false)
})
public class Msg_template extends BaseEntity implements Serializable {
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
    @Comment("渠道ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String channelId;

    @Column
    @Comment("渠道类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgChannelType channelType;

    @Column
    @Comment("提供商类型")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    private MsgProviderType providerType;

    @Column
    @Comment("模板业务类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgTemplateBizType bizType;

    @Column
    @Comment("模板名称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;

    @Column
    @Comment("模板代码")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String templateCode;

    @Column
    @Comment("模板内容")
    @ColDefine(type = ColType.TEXT)
    private String content;

    @Column
    @Comment("默认模板参数JSON")
    @ColDefine(type = ColType.TEXT)
    private String paramsJson;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;

    @One(field = "channelId")
    private Msg_channel channel;
}
