package com.budwk.sp.device.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备DTO")
public class DeviceInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;

    @NotBlank(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号长度不能超过64")
    private String deviceCode;

    @Size(max = 120, message = "设备名称长度不能超过120")
    private String name;

    @Size(max = 32, message = "IMEI长度不能超过32")
    private String imei;

    @Size(max = 32, message = "ICCID长度不能超过32")
    private String iccid;

    @NotBlank(message = "所属产品不能为空")
    private String productId;

    @Size(max = 64, message = "设备密钥长度不能超过64")
    private String secretKey;

    @Size(max = 255, message = "备注长度不能超过255")
    private String description;
    private boolean disabled;
}
