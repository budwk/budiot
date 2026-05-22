package com.budwk.sp.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备下行指令消息")
public class DeviceDownlinkCommandMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String commandId;
    private String commandLogId;
    private String tenantId;
    private String productId;
    private String productKey;
    private String deviceId;
    private String deviceCode;
    private String gatewayNodeId;
    private String networkProtocol;
    private String commandCode;
    private String payload;
    private String payloadJson;
    private boolean archiveCommandHistory = true;
    private boolean replyRequired;
    private Long deadlineAt;
}
