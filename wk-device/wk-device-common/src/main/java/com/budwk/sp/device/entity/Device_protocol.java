package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceProtocolScriptType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_protocol")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_DEVICE_PROTOCOL_CODE", fields = {"tenantId", "code"}, unique = true)})
public class Device_protocol extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Name @Column @Comment("ID") @ColDefine(type = ColType.VARCHAR, width = 32) @PrevInsert(els = {@EL("snowflake()")})
    private String id;
    @Column @Comment("租户ID") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;
    @Column @Comment("协议名称") @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;
    @Column @Comment("协议编码") @ColDefine(type = ColType.VARCHAR, width = 80)
    private String code;
    @Column @Comment("脚本类型") @ColDefine(type = ColType.VARCHAR, width = 30)
    private DeviceProtocolScriptType scriptType;
    @Column @Comment("解析脚本") @ColDefine(type = ColType.TEXT)
    private String scriptContent;
    @Column @Comment("脚本版本") @ColDefine(type = ColType.VARCHAR, width = 32)
    private String scriptVersion;
    @Column @Comment("协议说明") @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;
    @Column @Comment("是否禁用") @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
}
