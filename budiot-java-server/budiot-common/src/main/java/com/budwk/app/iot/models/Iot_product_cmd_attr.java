package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceDataType;
import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode
@Table("iot_product_cmd_attr")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品指令定义表")
@ApiModel(description = "产品指令定义")
public class Iot_product_cmd_attr implements Serializable {

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
    @ApiModelProperty(description = "产品ID")
    private String productId;

    @Column
    @Comment("指令配置ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "指令配置ID")
    private String cmdId;

    @Column
    @Comment("参数名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(description = "指令名称")
    private String name;

    @Column
    @Comment("参数标识")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "code", description = "参数标识(两个字符以上，并以字母开头，字母、数字、_、-组合，结尾为字母或数字)", required = true, check = true, regex = "^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$")
    private String code;

    @Column
    @Comment("数据类型")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "数据类型")
    private DeviceDataType dataType;

    @Column
    @Comment("默认值")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "默认值")
    private String defaultValue;
}
