package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.sys.enums.SysUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统单位 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统单位传输对象")
public class SysUnitDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "父级ID")
    private String parentId;

    @Schema(description = "树路径")
    private String path;

    @Schema(description = "单位类型")
    private SysUnitType type;

    @NotBlank(message = "单位名称不能为空")
    @Size(max = 100, message = "单位名称长度不能超过100")
    @Schema(description = "单位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 100, message = "单位别名长度不能超过100")
    @Schema(description = "单位别名")
    private String aliasName;

    @Size(max = 32, message = "机构编码长度不能超过32")
    @Schema(description = "机构编码")
    private String unitcode;

    @Size(max = 255, message = "单位logo长度不能超过255")
    @Schema(description = "单位logo")
    private String logo;

    @Size(max = 255, message = "单位介绍长度不能超过255")
    @Schema(description = "单位介绍")
    private String note;

    @Size(max = 255, message = "所属区域编号长度不能超过255")
    @Schema(description = "所属区域编号")
    private String areaCode;

    @Size(max = 255, message = "所属区域名称长度不能超过255")
    @Schema(description = "所属区域名称")
    private String areaText;

    @Size(max = 100, message = "单位地址长度不能超过100")
    @Schema(description = "单位地址")
    private String address;

    @Size(max = 20, message = "联系电话长度不能超过20")
    @Schema(description = "联系电话")
    private String telephone;

    @Size(max = 255, message = "单位邮箱长度不能超过255")
    @Schema(description = "单位邮箱")
    private String email;

    @Size(max = 255, message = "单位网站长度不能超过255")
    @Schema(description = "单位网站")
    private String website;

    @Size(max = 50, message = "负责人姓名长度不能超过50")
    @Schema(description = "负责人姓名")
    private String leaderName;

    @Size(max = 20, message = "负责人电话长度不能超过20")
    @Schema(description = "负责人电话")
    private String leaderMobile;

    @Schema(description = "是否禁用", defaultValue = "false")
    private boolean disabled;

    @Schema(description = "排序")
    private Integer location;

    @Schema(description = "是否有子节点")
    private boolean hasChildren;

    @Schema(description = "本单位领导(用户ID,逗号分隔)")
    private String leader;

    @Schema(description = "上级主管领导(用户ID,逗号分隔)")
    private String higher;

    @Schema(description = "上级分管领导(用户ID,逗号分隔)")
    private String assigner;
}
