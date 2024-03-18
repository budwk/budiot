package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceType;
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
@Table("iot_device")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备信息表")
@ApiModel(description = "设备信息")
public class Iot_device extends BaseModel implements Serializable {

    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("产品名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(name = "name", description = "产品名称", required = true)
    private String name;

    @Column
    @Comment("设备类型")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "deviceType", description = "设备类型")
    private DeviceType deviceType;

    @Column
    @Comment("设备分类")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "classifyId", description = "设备分类")
    private String classifyId;

    @Column
    @Comment("所属产品")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "productId", description = "所属产品")
    private String productId;

    @Column
    @Comment("设备协议")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "protocolId", description = "设备协议")
    private String protocolId;

    @Column
    @Comment("协议标识")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(name = "protocolCode", description = "协议标识")
    private String protocolCode;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("表号（表具通讯号）")
    @ApiModelProperty(description = "表号（表具通讯号）")
    private String meterNo;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("设备编号（铭牌号）")
    @ApiModelProperty(description = "设备编号")
    private String deviceNo;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("纬度")
    @ApiModelProperty(description = "纬度")
    private String lat;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("经度")
    @ApiModelProperty(description = "经度")
    private String lng;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("IMEI")
    private String imei;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("ICCID")
    private String iccid;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @Comment("第三方IOT平台设备id")
    private String iotPlatformId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Comment("所属单位")
    private String unitId;

    @Column
    @ColDefine(type = ColType.INT, width = 2)
    @Comment("设备状态")
    @ApiModelProperty(description = "设备状态：0-正常，1-异常")
    @Default("0")
    private Boolean abnormal;

    @Column
    @ColDefine(type = ColType.BOOLEAN)
    @Comment("在线状态")
    @ApiModelProperty(description = "在线状态：true-在线，false-不在线")
    @Default("0")
    private Boolean online;

    @Column
    @Comment("最新通讯连接时间")
    @ApiModelProperty(description = "最新通讯连接时间")
    private Long lastConnectionTime;
}
