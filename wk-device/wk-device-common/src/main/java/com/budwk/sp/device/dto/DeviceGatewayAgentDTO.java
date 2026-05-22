package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "设备网关Agent DTO")
public class DeviceGatewayAgentDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String agentId;
    private String host;
    private String status;
    private Long lastSeenAt;
    private Integer claimedGatewayCount = 0;
    private List<DeviceGatewayNodeDTO> gateways = new ArrayList<>();
}
