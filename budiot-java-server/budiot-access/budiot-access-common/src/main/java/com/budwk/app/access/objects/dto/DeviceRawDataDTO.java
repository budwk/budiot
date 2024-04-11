package com.budwk.app.access.objects.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设备原始报文（只用存mongo）
 */
@Data
public class DeviceRawDataDTO implements Serializable {
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
     * U 上行, D 下行
     */
    private String type;
    /**
     * 原始报文数据
     */
    private String originData;

    /**
     * 解析后的数据
     */
    private String parsedData;
    /**
     * 接收到的时间
     */
    private Long startTime;
    /**
     * 处理完成的时间
     */
    private Long endTime;

    /**
     * 存储id 用于回显
     */
    private String id;
}
