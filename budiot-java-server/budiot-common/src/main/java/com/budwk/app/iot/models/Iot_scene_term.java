package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.SceneTermType;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_scene_term")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("场景条件表")
public class Iot_scene_term extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    private String id;

    @Column
    @Comment("场景ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String sceneId;

    @Column
    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String productId;

    @Column
    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;

    @Column
    @Comment("设备名称")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String deviceName;

    @Column
    @Comment("条件类型")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private SceneTermType termType;

    @Column
    @Comment("参数名")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    private String paramName;

    @Column
    @Comment("参数值或范围")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String paramValue;

    @Column
    @Comment("持续时长（单位:秒，0为不配置）")
    @ColDefine(type = ColType.INT)
    private Integer duration;
}
