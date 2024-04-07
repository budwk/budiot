package com.budwk.app.access.objects.query;

import lombok.Data;

/**
 * 原始报文查询
 * @author wizzer.cn
 */
@Data
public class DeviceEventDataQuery extends PageQuery{
    private static final long serialVersionUID = 1L;

    //设备编号
    private String deviceNo;

    //设备表号
    private String meterNo;

    //设备id
    private String deviceId;

    //产品id
    private String productId;

    //开始时间戳
    private Long startTime;

    //结束时间戳
    private Long endTime;

    //ids
    private String ids;
}
