package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.excel.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "系统用户传输对象")
public class SysUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "用户ID", example = "", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "租户ID", example = "", accessMode = Schema.AccessMode.READ_ONLY)
    private String tenantId;

    @Schema(description = "员工编号", example = "1001", accessMode = Schema.AccessMode.READ_ONLY)
    @Excel(name = "用户编号", cellType = Excel.ColumnType.STRING, prompt = "用户编号", type = Excel.Type.IMPORT)
    private String serialNo;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名不能为空100")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @Excel(name = "用户登录名", type = Excel.Type.IMPORT)
    private String loginname;

    private String password;
    private String oldPassword;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名长度不能超过30")
    @Schema(description = "用户姓名", example = "老王", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Excel(name = "用户姓名", type = Excel.Type.IMPORT)
    private String username;

    @Schema(description = "用户性别", allowableValues = {"0", "1","2"}, defaultValue = "1")
    @Excel(name = "性别", dict = "1=男,2=女,0=未知", type = Excel.Type.IMPORT)
    private Integer sex;

    private String avatar;

    @Schema(description = "是否禁用", allowableValues = {"true", "false"}, defaultValue = "false")
    @Excel(name = "用户状态", dict = "true=禁用,false=启用", type = Excel.Type.IMPORT)
    private boolean disabled;

    @Schema(description = "邮箱地址", example = "abc@example.com", nullable = true)
    @Excel(name = "电子邮箱", type = Excel.Type.IMPORT)
    private String email;

    @Schema(description = "手机号码", example = "", nullable = true)
    @Excel(name = "手机号码", type = Excel.Type.IMPORT)
    private String mobile;

    @Schema(description = "所属单位ID")
    private String unitId;

    @Schema(description = "所属单位Path")
    private String unitPath;

    @Schema(description = "职务ID")
    private String postId;

    @Schema(description = "导入单位名称")
    @Excel(name = "单位名称", type = Excel.Type.IMPORT)
    private String importUnitName;

    @Schema(description = "角色ID数组")
    private String[] roleIds;
}
