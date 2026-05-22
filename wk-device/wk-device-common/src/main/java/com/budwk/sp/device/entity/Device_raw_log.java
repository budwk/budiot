package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceMessageDirection;
import com.budwk.sp.device.enums.DeviceMessageType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 设备原始报文归档表。
 * <p>
 * 用于保存设备上下行原始通信报文，表示设备侧网络报文留痕，不表示 MQ 中间消息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_raw_log")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_RAW_LOG_DEVICE", fields = {"tenantId", "deviceId", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_RAW_LOG_DIRECTION", fields = {"tenantId", "direction", "createdAt"}, unique = false),
        @Index(name = "INDEX_DEVICE_RAW_LOG_MESSAGE", fields = {"tenantId", "messageId"}, unique = false)
})
public class Device_raw_log extends BaseEntity implements Serializable {
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
    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;
    @Column
    @Comment("设备编号")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String deviceCode;
    @Column
    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;
    @Column
    @Comment("消息ID")
    @ColDefine(type = ColType.VARCHAR, width = 80)
    private String messageId;
    @Column
    @Comment("方向")
    @ColDefine(type = ColType.VARCHAR, width = 1)
    private DeviceMessageDirection direction;
    @Column
    @Comment("消息类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceMessageType messageType;
    @Column
    @Comment("网络协议")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String protocol;
    @Column
    @Comment("主题或通道")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String topic;
    @Column
    @Comment("网关节点")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String gatewayNodeId;
    @Column
    @Comment("原始报文内容")
    @ColDefine(type = ColType.TEXT)
    private String payload;
    @Column
    @Comment("解析结果JSON")
    @ColDefine(type = ColType.TEXT)
    private String parsedJson;
    @Column
    @Comment("是否成功")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean success;
    @Column
    @Comment("设备时间")
    private Long deviceAt;
}
