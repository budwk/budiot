package com.budwk.app.access.objects.dto;

import lombok.Data;
import org.nutz.dao.entity.annotation.ColDefine;
import org.nutz.dao.entity.annotation.ColType;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.entity.annotation.Id;

import java.io.Serializable;

/**
 * 设备上报数据基本信息
 * customType = "TAG" 时序数据库的TAG字段,可以为所属公司等固定数据
 *
 */
@Data
public class DeviceDataDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @ColDefine(type = ColType.TIMESTAMP)
    @Comment("时间主键")
    private Long ts;

    @ColDefine(type = ColType.TIMESTAMP)
    @Comment("接收数据时间")
    private Long receive_time;

    @Comment("产品ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String product_id;

    @Comment("设备通信号")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String device_no;

    @Comment("设备ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String device_id;

    @Comment("设备表号")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String meter_no;

    @Comment("设备协议标识")
    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String protocol_code;

    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String ext1;

    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String ext2;

    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String ext3;

    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String ext4;

    @ColDefine(type = ColType.VARCHAR, width = 32, customType = "TAG")
    private String ext5;
}
