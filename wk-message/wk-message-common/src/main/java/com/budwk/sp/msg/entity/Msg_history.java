package com.budwk.sp.msg.entity;

import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.enums.MsgSendStatus;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("msg_history_${month}")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Msg_history extends BaseEntity implements Serializable {
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
    @Comment("请求号")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String requestNo;

    @Column
    @Comment("渠道ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String channelId;

    @One(field = "channelId")
    private Msg_channel channel;

    @Column
    @Comment("模板ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String templateId;

    @Column
    @Comment("渠道类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgChannelType channelType;

    @Column
    @Comment("提供商类型")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    private MsgProviderType providerType;

    @Column
    @Comment("业务类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgTemplateBizType bizType;

    @Column
    @Comment("接收地址")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String receiver;

    @Column
    @Comment("接收人")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String receiverName;

    @Column
    @Comment("标题")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String title;

    @Column
    @Comment("内容")
    @ColDefine(type = ColType.TEXT)
    private String content;

    @Column
    @Comment("参数JSON")
    @ColDefine(type = ColType.TEXT)
    private String paramsJson;

    @Column
    @Comment("发送状态")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private MsgSendStatus status;

    @Column
    @Comment("供应商返回码")
    @ColDefine(type = ColType.VARCHAR, width = 80)
    private String providerCode;

    @Column
    @Comment("供应商返回消息")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String providerMsg;

    @Column
    @Comment("供应商请求ID")
    @ColDefine(type = ColType.VARCHAR, width = 128)
    private String providerRequestId;

    @Column
    @Comment("发送时间")
    private Long sendAt;

    @Column
    @Comment("成功时间")
    private Long successAt;

    @Column
    @Comment("失败时间")
    private Long failAt;

    public String getChannelName() {
        return channel == null ? "" : channel.getName();
    }
}
