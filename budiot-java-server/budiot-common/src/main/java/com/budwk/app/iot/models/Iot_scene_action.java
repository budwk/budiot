package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.SceneActionType;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_scene_action")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("场景动作表")
public class Iot_scene_action extends BaseModel implements Serializable {

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
    @Comment("动作类型")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private SceneActionType actionType;

    @Column
    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String deviceId;

    @Column
    @Comment("设备名称")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String deviceName;

    @Column
    @Comment("设备指令名")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    private String cmdCode;

    @Column
    @Comment("设备指令值")
    @ColDefine(type = ColType.VARCHAR, width = 500)
    private String cmdParam;

    @Column
    @Comment("手机号码")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String mobile;

    @Column
    @Comment("邮件地址")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String email;
}
