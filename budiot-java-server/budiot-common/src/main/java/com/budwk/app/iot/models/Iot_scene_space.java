package com.budwk.app.iot.models;

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
@Table("iot_scene_space")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("场景空间表")
@ApiModel(description = "场景空间表")
public class Iot_scene_space  extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("空间名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "name", description = "空间名称", required = true)
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
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM iot_scene_space"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM iot_scene_space")
    })
    @ApiModelProperty(description = "排序字段")
    private Integer location;

}
