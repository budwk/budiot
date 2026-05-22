package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备网关节点DTO")
public class DeviceGatewayNodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String gatewayId;
    private String nodeId;
    private String name;
    private String protocol;
    private String gatewayMode;
    private String protocolId;
    private String protocolName;
    private String host;
    private Integer port;
    private String path;
    private String remoteHost;
    private Integer remotePort;
    private Long lastSeenAt;
    private String status;
    private String gatewayProcessId;
    private boolean claimed;
    private String lastError;
}
