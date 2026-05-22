package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "系统应用传输对象")
public class SysAppDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;
    @NotBlank(message = "名称不能为空")
    @Size(max = 30, message = "名称长度不能超过30")
    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
    String name;
    String path;
    String icon;
    @Schema(description = "是否隐藏", allowableValues = {"true", "false"}, defaultValue = "false")
    boolean hidden;

    @Schema(description = "是否禁用", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean disabled;

    @Schema(description = "排序")
    private Integer location;
}
