package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceEventLevel;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 设备事件归档表。
 * <p>
 * 保存设备事件、告警、规则触发结果等事件型数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_event_log")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_EVENT_LOG_DEVICE", fields = {"tenantId", "deviceId", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_EVENT_LOG_SOURCE", fields = {"tenantId", "sourceType", "createdAt"}, unique = false)
})
public class Device_event_log extends BaseEntity implements Serializable {
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
    @Column @Comment("事件编码") @ColDefine(type = ColType.VARCHAR, width = 80)
    private String eventCode;
    @Column @Comment("事件名称") @ColDefine(type = ColType.VARCHAR, width = 120)
    private String eventName;
    @Column @Comment("事件级别") @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceEventLevel level;
    @Column @Comment("事件来源") @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceEventSourceType sourceType;
    @Column @Comment("事件内容JSON") @ColDefine(type = ColType.TEXT)
    private String contentJson;
    @Column @Comment("是否已处理") @ColDefine(type = ColType.BOOLEAN)
    private boolean handled;
    @Column @Comment("设备时间")
    private Long deviceAt;
}
