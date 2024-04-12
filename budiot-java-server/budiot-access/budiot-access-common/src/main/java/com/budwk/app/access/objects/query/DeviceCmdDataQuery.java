package com.budwk.app.access.objects.query;

import lombok.Data;

/**
 * @author wizzer.cn
 */
@Data
public class DeviceCmdDataQuery extends PageQuery{
    private static final long serialVersionUID = 1L;
    //产品id
    private String productId;

    //设备id
    private String deviceId;

    //开始时间戳
    private Long startTime;

    //结束时间戳
    private Long endTime;

    //ids
    private String ids;
}
