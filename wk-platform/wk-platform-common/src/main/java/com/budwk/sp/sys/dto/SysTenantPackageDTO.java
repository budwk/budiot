package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "租户套餐DTO")
public class SysTenantPackageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 100, message = "套餐名称长度不能超过100")
    @Schema(description = "套餐名称")
    private String name;

    @Size(max = 255, message = "套餐说明长度不能超过255")
    @Schema(description = "套餐说明")
    private String note;

    @Schema(description = "是否禁用")
    private boolean disabled;

    @Schema(description = "菜单ID数组")
    private String[] menuIds;
}
