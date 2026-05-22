package com.budwk.sp.device.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_vendor")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_DEVICE_VENDOR_CODE", fields = {"tenantId", "code"}, unique = true)})
public class Device_vendor extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Name @Column @Comment("ID") @ColDefine(type = ColType.VARCHAR, width = 32) @PrevInsert(els = {@EL("snowflake()")})
    private String id;
    @Column @Comment("租户ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;
    @Column @Comment("厂家名称") @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;
    @Column @Comment("厂家编码") @ColDefine(type = ColType.VARCHAR, width = 80)
    private String code;
    @Column @Comment("联系人") @ColDefine(type = ColType.VARCHAR, width = 60)
    private String contactName;
    @Column @Comment("联系电话") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String contactMobile;
    @Column @Comment("联系邮箱") @ColDefine(type = ColType.VARCHAR, width = 120)
    private String contactEmail;
    @Column @Comment("备注") @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;
    @Column @Comment("是否禁用") @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
}
