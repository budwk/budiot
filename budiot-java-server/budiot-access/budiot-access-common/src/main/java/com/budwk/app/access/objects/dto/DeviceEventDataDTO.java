package com.budwk.app.access.objects.dto;

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
     * 事件类型 0 信息，1 警告，2 故障, 3 恢复
     * INFO(0, "信息"),
     * ALARM(1, "报警"),
     * FAULT(2, "故障"),
     * RECOVER(3, "恢复");
     */
    private Integer eventType;
    /**
     * 0 原生告警，1 规则告警
     */
    private Integer sourceType;
    /**
     * 事件名称
     */
    private String eventName;
    /**
     * 发生告警时对应的告警值
     */
    private String warningValue;
    /**
     * 事件内容
     */
    private String content;
    /**
     * 事件数据
     */
    private List<ValueItemDTO<? extends Serializable>> eventData;
    /**
     * 存储id 用于回显
     */
    private String id;
}
