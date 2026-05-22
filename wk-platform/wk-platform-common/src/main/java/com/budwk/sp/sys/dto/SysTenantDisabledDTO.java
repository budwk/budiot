package com.budwk.sp.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "租户启用禁用DTO")
public class SysTenantDisabledDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "true=禁用")
    private boolean disabled;
}
