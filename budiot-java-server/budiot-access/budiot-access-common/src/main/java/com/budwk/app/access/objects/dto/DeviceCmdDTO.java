package com.budwk.app.access.objects.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DeviceCmdDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long ts;

    private String cmdId;

    private String deviceId;

    private String productId;

    private String deviceNo;

    private String code;

    private Map<String, Object> params;

    private Integer serialNo;

    private Integer status;

    private Long createTime;

    private Long sendTime;

    private Long finishTime;

    private String respResult;

    private String id;
}
