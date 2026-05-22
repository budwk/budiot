package com.budwk.sp.device.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.ColDefine;
import org.nutz.dao.entity.annotation.ColType;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.entity.annotation.EL;
import org.nutz.dao.entity.annotation.Index;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;
import org.nutz.dao.entity.annotation.TableIndexes;
import org.nutz.dao.entity.annotation.TableMeta;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_message_error")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_MESSAGE_ERROR_STATUS", fields = {"tenantId", "status", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_MESSAGE_ERROR_DEVICE", fields = {"tenantId", "deviceId", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_MESSAGE_ERROR_MESSAGE", fields = {"tenantId", "messageId"}, unique = false)
})
public class Device_message_error extends BaseEntity implements Serializable {
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
    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;

    @Column
    @Comment("产品Key")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productKey;

    @Column
    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;

    @Column
    @Comment("设备编号")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String deviceCode;

    @Column
    @Comment("消息场景")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    private String scene;

    @Column
    @Comment("消息模式")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String pattern;

    @Column
    @Comment("当前主题或通道")
    @ColDefine(type = ColType.VARCHAR, width = 160)
    private String topic;

    @Column
    @Comment("原始主题或通道")
    @ColDefine(type = ColType.VARCHAR, width = 160)
    private String originalTopic;

    @Column
    @Comment("消息ID")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String messageId;

    @Column
    @Comment("路由键")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String routingKey;

    @Column
    @Comment("原始消息JSON")
    @ColDefine(type = ColType.TEXT)
    private String rawMessage;

    @Column
    @Comment("消息载荷JSON")
    @ColDefine(type = ColType.TEXT)
    private String payloadJson;

    @Column
    @Comment("错误类型")
    @ColDefine(type = ColType.VARCHAR, width = 80)
    private String errorType;

    @Column
    @Comment("错误信息")
    @ColDefine(type = ColType.VARCHAR, width = 500)
    private String errorMessage;

    @Column
    @Comment("异常类名")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String exceptionClass;

    @Column
    @Comment("异常堆栈")
    @ColDefine(type = ColType.TEXT)
    private String stackTrace;

    @Column
    @Comment("重试次数")
    private Integer retryCount;

    @Column
    @Comment("最大重试次数")
    private Integer maxRetries;

    @Column
    @Comment("下次重试时间")
    private Long nextRetryAt;

    @Column
    @Comment("处理状态")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String status;

    @Column
    @Comment("消息发生时间")
    private Long occurredAt;
}
