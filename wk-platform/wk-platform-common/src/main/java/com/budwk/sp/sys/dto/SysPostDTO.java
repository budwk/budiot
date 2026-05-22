package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.excel.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统员工职务 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统员工职务传输对象")
public class SysPostDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @NotBlank(message = "职务名称不能为空")
    @Size(max = 50, message = "职务名称长度不能超过50")
    @Schema(description = "职务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Excel(name = "职务名称", type = Excel.Type.IMPORT)
    private String name;

    @Size(max = 25, message = "职务编号长度不能超过25")
    @Schema(description = "职务编号")
    @Excel(name = "职务编号", type = Excel.Type.IMPORT)
    private String code;

    @Schema(description = "排序")
    private Integer location;
}
