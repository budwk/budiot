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
@Table("iot_device_log")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备操作日志表")
@ApiModel(description = "设备操作日志")
public class Iot_device_log extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("设备ID")
    @ApiModelProperty(description = "设备ID")
    private String deviceId;

    @Column
    @Comment("操作时间")
    @ApiModelProperty(description = "操作时间")
    private Long operateTime;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("操作人ID")
    @ApiModelProperty(description = "操作人ID")
    private String operatorId;

    @Column
    @Comment("操作人名称")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @ApiModelProperty(description = "操作人名称")
    private String operatorName;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 300)
    @Comment("描述")
    @ApiModelProperty(description = "描述")
    private String note;
}
