package com.budwk.app.access.processor.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备聚合信息缓存
 */
@Data
public class DeviceProcessCache implements Serializable {
    // 所属公司(扩展用)
    private String companyId;
    private String productId;
    // 所属集中器ID/采集器ID
    private String collectorId;
    private String deviceId;
    private String meterNo;
    private String deviceNo;
    // 阀门状态
    private Integer valveState;
    // 缓存时间
    private Long refreshTime;

}
