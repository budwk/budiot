package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "协议身份预解析请求")
public class DeviceProtocolIdentityResolveRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String protocolId;
    private String productKey;
    private String deviceCode;
    private String networkProtocol;
    private String endpoint;
    private String sourceIp;
    private String payload;
    private Long receivedAt;
    private Map<String, String> headers = new HashMap<>();
}
