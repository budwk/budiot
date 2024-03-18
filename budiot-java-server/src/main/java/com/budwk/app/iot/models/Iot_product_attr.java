package com.budwk.app.iot.models;

import com.budwk.app.iot.enums.DeviceAttrType;
import com.budwk.app.iot.enums.DeviceDataType;
import com.budwk.app.iot.objects.dto.ValueTextDTO;
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
@Table("iot_product_attr")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@Comment("设备上报参数配置表")
@ApiModel(description = "设备上报参数配置")
public class Iot_product_attr extends BaseModel implements Serializable {
    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    @ApiModelProperty(description = "id")
    private String id;

    @Column
    @Comment("产品id")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "产品id")
    private String productId;

    @Column
    @Comment("名称")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "名称")
    private String name;

    @Column
    @Comment("标识")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @ApiModelProperty(description = "标识")
    private String code;

    @Column
    @Comment("数据类型")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "数据类型")
    private DeviceDataType dataType;

    @Column
    @Comment("参数类型")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "参数类型")
    private DeviceAttrType attrType;

    @Column
    @Comment("字节长度")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "字节长度")
    private Integer dataLen;

    @Column
    @Comment("小数位数")
    @ColDefine(type = ColType.INT)
    @ApiModelProperty(description = "小数位数")
    private Integer scale;

    @Column
    @Comment("单位")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    @ApiModelProperty(description = "单位")
    private String unit;

    @Column
    @Comment("最小值")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    @ApiModelProperty(description = "最小值")
    private String minimum;

    @Column
    @Comment("最大值")
    @ColDefine(type = ColType.VARCHAR, width = 30)
    @ApiModelProperty(description = "最大值")
    private String maximum;

    @Column
    @Comment("枚举类型JSON扩展存储")
    @ColDefine(type = ColType.MYSQL_JSON)
    @ApiModelProperty(description = "枚举键值对类型扩展")
    private List<ValueTextDTO<?>> ext;

    @Column
    @Comment("说明")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @ApiModelProperty(description = "说明")
    private String note;

    @Column
    @Comment("排序")
    @ColDefine(type = ColType.INT)
    private Integer location;

}
