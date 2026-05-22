package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备实时状态DTO")
public class DeviceRuntimeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String deviceId; private boolean online; private String ip; private Long lastHeartbeatAt; private Long lastDeviceAt; private String gatewayNodeId;
}
