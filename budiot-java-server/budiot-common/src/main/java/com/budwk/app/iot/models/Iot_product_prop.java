package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceAttrType;
import com.budwk.app.iot.enums.DeviceDataType;
import com.budwk.app.iot.objects.dto.ValueTextDTO;
import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_product_prop")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品属性配置表")
@ApiModel(description = "产品属性配置")
@TableIndexes({@Index(name = "IDX_PRO_PROP", fields = {"productId", "code"}, unique = true)})
public class Iot_product_prop extends BaseModel implements Serializable {
    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("产品id")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "产品id")
    private String productId;

    @Column
    @Comment("名称")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "名称")
    private String name;

    @Column
    @Comment("标识")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "code", description = "属性标识(两个字符以上，并以字母开头，字母、数字、_、-组合，结尾为字母或数字)", required = true, check = true, regex = "^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$")
    private String code;

    @Column
    @Comment("默认值")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "默认值")
    private String defaultValue;

    @Column
    @Comment("是否必填")
    @ColDefine(type = ColType.BOOLEAN)
    @ApiModelProperty(description = "是否必填")
    private Boolean required;

    @Column
    @Comment("说明")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @ApiModelProperty(description = "说明")
    private String note;

    @Column
    @Comment("排序")
    @ColDefine(type = ColType.INT)
    @Prev({
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM iot_product_prop"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM iot_product_prop")
    })
    private Integer location;
}
