package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 数据字典 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "数据字典传输对象")
public class SysDictDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "父级ID")
    private String parentId;

    @Schema(description = "树路径")
    private String path;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100")
    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 20, message = "字典编码长度不能超过20")
    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "是否禁用", defaultValue = "false")
    private boolean disabled;

    @Schema(description = "排序")
    private Integer location;

    @Schema(description = "是否有子节点")
    private boolean hasChildren;
}
