package com.budwk.app.iot.models;

import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_scene")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("场景表")
@ApiModel(description = "场景表")
public class Iot_scene extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("空间ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "空间ID")
    private String spaceId;

    @Column
    @Comment("场景名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "name", description = "场景名称", required = true)
    private String name;

    @Column
    @Comment("场景说明")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @ApiModelProperty(name = "name", description = "场景说明", required = true)
    private String note;

    @Column
    @Comment("是否启用")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("true")
    @ApiModelProperty(description = "是否启用")
    private Boolean enabled;

    @Column
    @Comment("生效时间")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("false")
    @ApiModelProperty(description = "生效时间")
    private Boolean hasTimeRange;

    @Column
    @Comment("时间起")
    @ColDefine(type = ColType.VARCHAR, width = 10)
    @ApiModelProperty(name = "timeStart", description = "时间起")
    private String timeStart;

    @Column
    @Comment("时间至")
    @ColDefine(type = ColType.VARCHAR, width = 10)
    @ApiModelProperty(name = "timeEnd", description = "时间至")
    private String timeEnd;

    @Many(field = "sceneId")
    private List<Iot_scene_term> terms;

    @Many(field = "sceneId")
    private List<Iot_scene_action> actions;
}
