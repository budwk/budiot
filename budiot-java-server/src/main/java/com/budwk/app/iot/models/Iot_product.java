package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceType;
import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.enums.ProtocolType;
import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_product")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品信息表")
@ApiModel(description = "产品信息")
public class Iot_product extends BaseModel implements Serializable {
    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("产品名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "name", description = "产品名称", required = true)
    private String name;

    @Column
    @Comment("设备类型")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "deviceType", description = "设备类型")
    private DeviceType deviceType;

    @Column
    @Comment("设备分类")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "classifyId", description = "设备分类")
    private String classifyId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 10)
    @Comment("协议类型")
    @ApiModelProperty(description = "协议类型")
    private ProtocolType protocolType;

    @Column
    @Comment("设备协议")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "protocolId", description = "设备协议")
    private String protocolId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 10)
    @Comment("接入平台")
    @ApiModelProperty(name = "iotPlatform", description = "接入平台")
    private IotPlatform iotPlatform;

    @Column
    @ColDefine(type = ColType.INT)
    @Comment("计费方式")
    @ApiModelProperty(description = "计费方式(1-表端计费，2-平台计费）")
    private Integer payMode;

    @Column
    @Comment("扩展配置")
    @ColDefine(type = ColType.MYSQL_JSON)
    private Map<String, String> properties;

    @Column
    @Comment("备注")
    @ColDefine(type = ColType.VARCHAR, width = 300)
    @ApiModelProperty(name = "description", description = "备注")
    private String description;

    @One(field = "protocolId")
    private Iot_protocol protocol;

    @One(field = "classifyId")
    private Iot_classify classify;
}
