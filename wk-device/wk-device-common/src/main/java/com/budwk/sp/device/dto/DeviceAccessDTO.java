package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备接入信息")
public class DeviceAccessDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String tenantId;
    private String deviceCode;
    private String productId;
    private String productKey;
    private String secretKey;
    private boolean disabled;
}
