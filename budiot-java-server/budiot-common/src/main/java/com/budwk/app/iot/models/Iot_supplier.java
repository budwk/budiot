package com.budwk.app.iot.models;

import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_supplier")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备厂家表")
@ApiModel(description = "设备厂家")
public class Iot_supplier extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("厂家名称")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "name", description = "协议名称", required = true)
    private String name;

    @Column
    @Comment("厂家标识")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "code", description = "厂家标识(字母、数字、_、-，以字母开头，结尾为字母或数字)", required = true, check = true, regex = "^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$")
    private String code;

}
