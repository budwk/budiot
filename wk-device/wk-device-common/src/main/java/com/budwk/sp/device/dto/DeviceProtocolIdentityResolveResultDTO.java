package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议身份预解析结果")
public class DeviceProtocolIdentityResolveResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String identityType;
    private String identityValue;
    private String productKey;
    private String deviceCode;
    private String imei;
    private String iccid;
}
