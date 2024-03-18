package com.budwk.app.iot.models;

import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.database.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_device_property")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备属性表")
@ApiModel(description = "设备属性")
public class Iot_device_prop extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("设备id")
    private String deviceId;

    @Column
    @Comment("json配置")
    @ColDefine(type = ColType.MYSQL_JSON)
    private Map<String, Object> properties;
}
