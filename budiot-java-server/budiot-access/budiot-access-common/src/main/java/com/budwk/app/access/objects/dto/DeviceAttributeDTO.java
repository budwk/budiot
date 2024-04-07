package com.budwk.app.access.objects.dto;

import com.budwk.app.iot.enums.DeviceDataType;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备参数
 */
@Data
public class DeviceAttributeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    //参数名
    private String name;
    //标识符
    private String code;
    //数据类型
    private DeviceDataType dataType;
    //小数位
    private Integer scale;
    //单位
    private String unit;

    public int getDataTypeValue() {
        return dataType.getValue();
    }
}
