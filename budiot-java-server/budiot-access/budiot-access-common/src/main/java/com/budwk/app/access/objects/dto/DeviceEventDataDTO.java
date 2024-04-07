package com.budwk.app.access.objects.dto;

import com.budwk.app.iot.enums.DeviceEventType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设备事件（只用存mongo）
 */
@Data
public class DeviceEventDataDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 存入数据库的时间戳
     */
    private long ts;

    private String deviceId;
    private String deviceNo;
    private String meterNo;
    private String productId;
    /**
     * 事件时间
     */
    private Long eventTime;
    /**
     * 事件类型
     */
    private DeviceEventType eventType;
    /**
     * 事件内容
     */
    private String content;
    /**
     * 事件数据
     */
    private List<Map<String,Object>> eventData;
    /**
     * 存储id 用于回显
     */
    private String id;
}
