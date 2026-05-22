package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.enums.DeviceProductType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_product")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_PRODUCT_KEY", fields = {"tenantId", "productKey"}, unique = true),
        @Index(name = "INDEX_DEVICE_PRODUCT_CATEGORY", fields = {"tenantId", "categoryId"}, unique = false)
})
public class Device_product extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Name @Column @Comment("ID") @ColDefine(type = ColType.VARCHAR, width = 32) @PrevInsert(els = {@EL("snowflake()")})
    private String id;
    @Column @Comment("租户ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;
    @Column @Comment("产品Key") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productKey;
    @Column @Comment("产品名称") @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;
    @Column @Comment("分类ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String categoryId;
    @Column @Comment("厂家ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String vendorId;
    @Column @Comment("产品类型") @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceProductType productType;
    @Column @Comment("网络协议") @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceNetworkProtocol networkProtocol;
    @Column @Comment("协议ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String protocolId;
    @Column @Comment("目标网关节点") @ColDefine(type = ColType.VARCHAR, width = 64)
    private String gatewayNodeId;
    @Column @Comment("网关端口")
    private Integer gatewayPort;
    @Column @Comment("物模型属性JSON") @ColDefine(type = ColType.TEXT)
    private String thingPropertyJson;
    @Column @Comment("物模型服务JSON") @ColDefine(type = ColType.TEXT)
    private String thingServiceJson;
    @Column @Comment("物模型事件JSON") @ColDefine(type = ColType.TEXT)
    private String thingEventJson;
    @Column @Comment("说明") @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;
    @Column @Comment("是否禁用") @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
    @One(field = "categoryId") private Device_category category;
    @One(field = "vendorId") private Device_vendor vendor;
    @One(field = "protocolId") private Device_protocol protocol;
    private Integer deviceCount;
    private String gatewayName;
    private String gatewayStatus;
}
