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
@Table("iot_product_firmware")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("产品固件表")
@ApiModel(description = "产品固件")
@TableIndexes({@Index(name = "IDX_PRO_FIRMWARE", fields = {"productId"}, unique = false)})
public class Iot_product_firmware extends BaseModel implements Serializable {
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
    @Comment("固件名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @ApiModelProperty(description = "固件名称")
    private String name;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @Comment("固件版本号")
    @ApiModelProperty(description = "固件版本号")
    private String version;

    @Column
    @Comment("是否启用")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("1")
    @ApiModelProperty(description = "是否启用")
    private Boolean enabled;

    @Column
    @Comment("升级所有")
    @ColDefine(type = ColType.BOOLEAN)
    @Default("0")
    @ApiModelProperty(description = "是否所有设备都需要升级")
    private Boolean allUpgrade;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @Comment("固件文件路径")
    @ApiModelProperty(description = "固件文件路径")
    private String path;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @Comment("固件说明")
    @ApiModelProperty(description = "固件说明")
    private String note;
}
