package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "设备指令DTO")
public class DeviceCommandDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;
    private String deviceId;
    private List<String> deviceIds;
    private String productId;

    @NotBlank(message = "指令编码不能为空")
    @Size(max = 80, message = "指令编码长度不能超过80")
    private String commandCode;

    @NotBlank(message = "指令内容不能为空")
    private String payloadJson;

    private boolean replyRequired;
    private Long deadlineAt;
    private DeviceCommandStatus status;
}
