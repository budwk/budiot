package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.SubscribeType;
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
@Table("iot_product_sub")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品订阅表")
@ApiModel(description = "产品订阅")
public class Iot_product_sub extends BaseModel implements Serializable {
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
    @Comment("订阅地址")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @ApiModelProperty(description = "订阅地址")
    private String url;

    @Column
    @Comment("订阅类别")
    @ColDefine(type = ColType.INT)
    private SubscribeType subType;

    @Column
    @Comment("是否启用")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("1")
    @ApiModelProperty(description = "是否启用")
    private Boolean enabled;
}
