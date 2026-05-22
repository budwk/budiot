package com.budwk.sp.device.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备厂家DTO")
public class DeviceVendorDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;

    @NotBlank(message = "厂家名称不能为空")
    @Size(max = 120, message = "厂家名称长度不能超过120")
    private String name;

    @NotBlank(message = "厂家编码不能为空")
    @Size(max = 80, message = "厂家编码长度不能超过80")
    private String code;

    @Size(max = 60, message = "联系人长度不能超过60")
    private String contactName;

    @Size(max = 32, message = "联系电话长度不能超过32")
    private String contactMobile;

    @Size(max = 120, message = "联系邮箱长度不能超过120")
    private String contactEmail;

    @Size(max = 255, message = "备注长度不能超过255")
    private String description;

    private boolean disabled;
}
