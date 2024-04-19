package com.budwk.app.iot.objects.cache;

import com.budwk.app.iot.enums.DeviceDataType;
import com.budwk.app.iot.enums.DeviceType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据处理链-设备缓存对象
 */
@Data
public class DeviceProcessCache implements Serializable {
    private String companyId;//预留
    /**
     * 设备基本信息
     */
    private String deviceId;
    private String parentId;
    private String meterNo;
    private String deviceNo;
    private String productId;
    private String classifyId;
    private Integer valveState;
    /**
     * 上次接收时间
     */
    private Long lastReceiveTime;
    /**
     * 异常相关
     */
    private boolean abnormal;
    private Long abnormalTime;
    private Long lastDataTime;
    private Integer payMode;
    // 设备协议
    private String protocolCode;
    // 缓存时间记录
    private Long refreshTime;
    // 产品属性配置
    private Map<String, String> productProperties;
    private DeviceType deviceType;
    // json or
    private String dataFormat;
}
