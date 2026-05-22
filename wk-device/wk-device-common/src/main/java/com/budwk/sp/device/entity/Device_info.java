package com.budwk.sp.device.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_info")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_INFO_CODE", fields = {"tenantId", "deviceCode"}, unique = true),
        @Index(name = "INDEX_DEVICE_INFO_PRODUCT", fields = {"tenantId", "productId"}, unique = false)
})
public class Device_info extends BaseEntity implements Serializable {
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
    @Comment("设备编号")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String deviceCode;
    @Column
    @Comment("设备名称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;
    @Column
    @Comment("IMEI")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String imei;
    @Column
    @Comment("ICCID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String iccid;
    @Column
    @Comment("所属产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;
    @Column
    @Comment("所属产品Key")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productKey;
    @Column
    @Comment("设备密钥")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String secretKey;
    @Column
    @Comment("备注")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;
    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
    @One(field = "productId")
    private Device_product product;
    private Boolean online;
    private String ip;
    private Long lastHeartbeatAt;
    private Long lastDeviceAt;
    private String gatewayNodeId;
}
