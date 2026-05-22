package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceProtocolScriptType;
import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备协议DTO")
public class DeviceProtocolDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;

    @NotBlank(message = "协议名称不能为空")
    @Size(max = 120, message = "协议名称长度不能超过120")
    private String name;

    @NotBlank(message = "协议编码不能为空")
    @Size(max = 80, message = "协议编码长度不能超过80")
    private String code;

    @NotNull(message = "脚本类型不能为空")
    private DeviceProtocolScriptType scriptType;

    @NotBlank(message = "解析脚本不能为空")
    private String scriptContent;

    @Size(max = 32, message = "脚本版本长度不能超过32")
    private String scriptVersion;

    @Size(max = 255, message = "协议说明长度不能超过255")
    private String description;

    private boolean disabled;
}
