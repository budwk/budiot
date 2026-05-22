package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "租户DTO")
public class SysTenantDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称长度不能超过100")
    @Schema(description = "租户名称")
    private String name;

    @NotBlank(message = "租户套餐不能为空")
    @Schema(description = "套餐ID")
    private String packageId;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "是否过期")
    private boolean hasExpire;

    @Schema(description = "过期时间")
    private Long expireAt;

    @Schema(description = "是否禁用")
    private boolean disabled;

    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 120, message = "管理员账号长度不能超过120")
    @Schema(description = "管理员账号")
    private String adminLoginname;

    @Schema(description = "管理员密码")
    private String adminPassword;
}
