package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceGatewayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备网关控制消息")
public class DeviceGatewayControlMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String gatewayId;
    private String gatewayName;
    private String tenantId;
    private DeviceGatewayStatus targetStatus;
    private String action; // 动作类型：START/STOP/CONFIG_UPDATE
    private String operatorId;
}
