package com.budwk.app.iot.models;

import com.budwk.starter.common.enums.Validation;
import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_classify")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备分类表")
@ApiModel(description = "设备分类")
public class Iot_classify extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("分类名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "name", description = "分类名称", required = true)
    private String name;

    @Column
    @Comment("父级ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "parentId", description = "父级ID")
    private String parentId;

    @Column
    @Comment("树路径")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @ApiModelProperty(name = "path", description = "树路径")
    private String path;

    @Column
    @Comment("有子节点")
    @ApiModelProperty(name = "hasChildren", description = "有子节点")
    private boolean hasChildren;

    @Column
    @Comment("排序字段")
    @Prev({
            @SQL(db= DB.MYSQL,value = "SELECT IFNULL(MAX(location),0)+1 FROM iot_classify"),
            @SQL(db= DB.ORACLE,value = "SELECT COALESCE(MAX(location),0)+1 FROM iot_classify")
    })
    @ApiModelProperty(description = "排序字段")
    private Integer location;

    @Column
    @Comment("图标")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "icon", description = "图标")
    private String icon;

    @Column
    @Comment("颜色")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "color", description = "颜色")
    private String color;
}
