package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 设备指令历史归档表。
 * <p>
 * 保存下行指令的排队、执行、回复、失败等完整历史，不包含当前待发送队列表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_command_log")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_DEVICE_COMMAND_LOG_DEVICE", fields = {"tenantId", "deviceId", "createdAt"}, unique = false)})
public class Device_command_log extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Name @Column @Comment("ID") @ColDefine(type = ColType.VARCHAR, width = 32) @PrevInsert(els = {@EL("snowflake()")})
    private String id;
    @Column @Comment("租户ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;
    @Column @Comment("设备ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;
    @Column @Comment("设备编号") @ColDefine(type = ColType.VARCHAR, width = 64)
    private String deviceCode;
    @Column @Comment("产品ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;
    @Column @Comment("指令编码") @ColDefine(type = ColType.VARCHAR, width = 80)
    private String commandCode;
    @Column @Comment("指令内容JSON") @ColDefine(type = ColType.TEXT)
    private String payloadJson;
    @Column @Comment("是否需要回复") @ColDefine(type = ColType.BOOLEAN)
    private boolean replyRequired;
    @Column @Comment("截止时间")
    private Long deadlineAt;
    @Column @Comment("消息ID") @ColDefine(type = ColType.VARCHAR, width = 64)
    private String messageId;
    @Column @Comment("消息主题") @ColDefine(type = ColType.VARCHAR, width = 160)
    private String messageTopic;
    @Column @Comment("排队时间")
    private Long queuedAt;
    @Column @Comment("下发时间")
    private Long sentAt;
    @Column @Comment("完成时间")
    private Long finishedAt;
    @Column @Comment("回复内容JSON") @ColDefine(type = ColType.TEXT)
    private String responseJson;
    @Column @Comment("错误信息") @ColDefine(type = ColType.VARCHAR, width = 255)
    private String errorMessage;
    @Column @Comment("状态") @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceCommandStatus status;
}
