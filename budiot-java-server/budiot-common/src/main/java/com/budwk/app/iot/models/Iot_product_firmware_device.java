package com.budwk.app.iot.models;

import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_product_firmware_device")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("固件设备关联表")
public class Iot_product_firmware_device extends BaseModel implements Serializable {
    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL(value = "snowflake()")})
    @Comment("ID")
    @ApiModelProperty(description = "ID")
    private String id;

    @Column
    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;

    @Column
    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;

    @Column
    @Comment("设备通信号")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceNo;

    @Column
    @Comment("设备IMEI")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String imei;
}
