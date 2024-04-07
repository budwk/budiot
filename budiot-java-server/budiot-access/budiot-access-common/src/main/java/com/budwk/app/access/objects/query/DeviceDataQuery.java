package com.budwk.app.access.objects.query;

import lombok.Data;

/**
 * @author wizzer.cn
 */
@Data
public class DeviceDataQuery extends PageQuery{
    private static final long serialVersionUID = 1L;

    //设备协议
    private String protocolCode;

    //设备id
    private String deviceId;

    //开始时间戳
    private Long startTime;

    //结束时间戳
    private Long endTime;

    //ids
    private String ids;
}
