package com.budwk.sp.device.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 设备解析数据归档表。
 * <p>
 * 保存协议脚本解析后的属性、测点、时序值等有效业务数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_data_log")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_DATA_LOG_DEVICE", fields = {"tenantId", "deviceId", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_DATA_LOG_MESSAGE", fields = {"tenantId", "messageId"}, unique = false)
})
public class Device_data_log extends BaseEntity implements Serializable {
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
    @Column @Comment("消息ID") @ColDefine(type = ColType.VARCHAR, width = 80)
    private String messageId;
    @Comment("数据标识符（非关系库存储兼容字段）")
    private String identifier;
    @Comment("数据名称（非关系库存储兼容字段）")
    private String name;
    @Comment("数据值JSON（非关系库存储兼容字段）")
    private String valueJson;
    @Comment("单位（非关系库存储兼容字段）")
    private String unit;
    @Column @Comment("设备时间")
    private Long deviceAt;
}
