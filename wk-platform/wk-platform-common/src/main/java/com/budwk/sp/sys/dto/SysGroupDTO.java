package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 角色分组 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "角色分组传输对象")
public class SysGroupDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "单位ID")
    private String unitId;

    @Schema(description = "单位PATH")
    private String unitPath;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称长度不能超过50")
    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
