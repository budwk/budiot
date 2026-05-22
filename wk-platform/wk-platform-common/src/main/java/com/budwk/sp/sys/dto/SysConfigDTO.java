package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.sys.enums.SysConfigType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统参数配置 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统参数配置传输对象")
public class SysConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "参数类型")
    private SysConfigType type;

    @NotBlank(message = "配置Key不能为空")
    @Size(max = 125, message = "配置Key长度不能超过125")
    @Schema(description = "配置Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    @Size(max = 125, message = "配置Value长度不能超过125")
    @Schema(description = "配置Value")
    private String configValue;

    @Size(max = 255, message = "配置说明长度不能超过255")
    @Schema(description = "配置说明")
    private String note;

    @Schema(description = "是否开放", defaultValue = "false")
    private boolean opened;
}
