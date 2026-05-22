package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceCommandStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备指令状态更新消息")
public class DeviceCommandStatusUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String commandId;
    private String messageId;
    private DeviceCommandStatus status;
    private Long sentAt;
    private String responseJson;
    private String errorMessage;
    private Long finishedAt;
    private String updatedBy;
}
