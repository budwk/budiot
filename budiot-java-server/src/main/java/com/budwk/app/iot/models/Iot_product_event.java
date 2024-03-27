package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceEventType;
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
@Table("iot_product_event")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品事件表")
@ApiModel(description = "产品事件")
@TableIndexes({@Index(name = "IDX_PRO_EVENT", fields = {"productId", "code"}, unique = true)})
public class Iot_product_event extends BaseModel implements Serializable {
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
    @Comment("指令名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(description = "指令名称")
    private String name;

    @Column
    @Comment("指令标识")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "code", description = "指令标识(两个字符以上，并以字母开头，字母、数字、_、-组合，结尾为字母或数字)", required = true, check = true, regex = "^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$")
    private String code;

    @Column
    @Comment("事件类型")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "事件类型")
    @Default("0")
    private DeviceEventType eventType;

    @Column
    @Comment("说明")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @ApiModelProperty(description = "说明")
    private String note;
}
