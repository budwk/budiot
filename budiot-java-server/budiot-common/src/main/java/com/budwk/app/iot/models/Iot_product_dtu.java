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
@Table("iot_product_dtu")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品DTU配置表")
@ApiModel(description = "产品DTU配置表")
@TableIndexes({@Index(name = "IDX_PRO_DTU", fields = {"productId"}, unique = true)})
public class Iot_product_dtu extends BaseModel implements Serializable {
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
    @Comment("是否启用")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("1")
    @ApiModelProperty(description = "是否启用")
    private Boolean enabled;

    @Column
    @ColDefine(type = ColType.INT)
    @Comment("参数版本号")
    @ApiModelProperty(description = "参数版本号")
    private Integer version;

    @Column
    @Comment("json参数配置")
    @ColDefine(type = ColType.MYSQL_JSON)
    @ApiModelProperty(description = "json参数配置")
    private String config;
}
