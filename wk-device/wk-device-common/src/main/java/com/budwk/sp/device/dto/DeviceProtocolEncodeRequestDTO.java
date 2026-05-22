package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceProtocolScriptType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "协议脚本编码请求")
public class DeviceProtocolEncodeRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "脚本类型不能为空")
    private DeviceProtocolScriptType scriptType;

    @NotBlank(message = "脚本内容不能为空")
    private String scriptContent;

    @NotBlank(message = "指令JSON不能为空")
    private String commandJson;
}
