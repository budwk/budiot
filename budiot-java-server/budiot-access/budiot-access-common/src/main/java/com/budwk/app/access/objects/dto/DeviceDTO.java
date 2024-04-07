package com.budwk.app.access.objects.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备字段
 */
@Data
public class DeviceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String deviceNo;
    private String meterNo;
    private String productId;
    /**
     * 设备协议解析标识
     */
    private String protocolCode;
}
