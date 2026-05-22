package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统角色 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统角色传输对象")
public class SysRoleDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 255, message = "角色代码长度不能超过255")
    @Schema(description = "角色代码")
    private String code;

    @Schema(description = "是否禁用", defaultValue = "false")
    private boolean disabled;

    @Schema(description = "单位ID")
    private String unitId;

    @Schema(description = "分组ID")
    private String groupId;

    @Size(max = 255, message = "角色备注长度不能超过255")
    @Schema(description = "角色备注")
    private String note;
}
