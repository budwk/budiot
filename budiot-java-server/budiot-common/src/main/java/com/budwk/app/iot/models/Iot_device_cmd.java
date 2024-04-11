package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.starter.common.openapi.annotation.ApiModel;
import com.budwk.starter.common.openapi.annotation.ApiModelProperty;
import com.budwk.starter.database.model.BaseModel;
import com.budwk.starter.excel.annotation.Excel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("iot_device_cmd")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "IDX_DEVICE_CMD", fields = {"deviceId"}, unique = false)})
@Comment("设备指令表")
@ApiModel(description = "设备指令表(待下发)")
public class Iot_device_cmd extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("设备id")
    @ApiModelProperty(description = "设备id", required = true)
    private String deviceId;

    @Column
    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "产品ID")
    private String productId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("设备通讯号")
    @ApiModelProperty(description = "设备通讯号", required = true)
    private String deviceNo;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("指令标识")
    @ApiModelProperty(description = "指令标识")
    private String code;

    @Column
    @ColDefine(type = ColType.MYSQL_JSON)
    @Comment("指令参数")
    @ApiModelProperty(description = "指令参数")
    private Map<String, Object> params;

    /**
     * 有些表协议需要根据这个来查询指令
     */
    @Column
    @Comment("指令序号")
    @ApiModelProperty(description = "指令序号")
    private Integer serialNo;

    @Column
    @Comment("指令状态")
    @ColDefine(type = ColType.INT, width = 1)
    @ApiModelProperty(description = "指令状态")
    private DeviceCmdStatus status;

    @Column
    @Comment("创建时间")
    @ApiModelProperty(description = "创建时间")
    private Long createTime;

    @Column
    @Comment("下发时间")
    @ApiModelProperty(description = "下发时间")
    private Long sendTime;

    @Column
    @Comment("结束时间")
    @ApiModelProperty(description = "结束时间")
    private Long finishTime;
}
