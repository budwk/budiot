package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.nutz.lang.util.NutMap;

import java.util.List;

import java.io.Serializable;
/**
 * 菜单 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "菜单传输对象")
public class SysMenuDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "父级ID")
    private String parentId;

    @Schema(description = "树路径")
    private String path;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100, message = "菜单名称长度不能超过100")
    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 100, message = "多语言标识符长度不能超过100")
    @Schema(description = "多语言标识符")
    private String alias;

    @Size(max = 10, message = "资源类型长度不能超过10")
    @Schema(description = "资源类型: menu-菜单, button-按钮")
    private String type;

    @Size(max = 255, message = "菜单链接长度不能超过255")
    @Schema(description = "菜单链接")
    private String href;

    @Size(max = 50, message = "打开方式长度不能超过50")
    @Schema(description = "打开方式")
    private String target;

    @Size(max = 50, message = "菜单图标长度不能超过50")
    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "是否显示", defaultValue = "true")
    private boolean showit;

    @Schema(description = "是否禁用", defaultValue = "false")
    private boolean disabled;

    @Size(max = 255, message = "权限标识长度不能超过255")
    @Schema(description = "权限标识")
    private String permission;

    @Size(max = 255, message = "菜单介绍长度不能超过255")
    @Schema(description = "菜单介绍")
    private String note;

    @Schema(description = "排序")
    private Integer location;

    @Schema(description = "是否有子节点")
    private boolean hasChildren;

    @Schema(description = "权限按钮数组")
    private List<NutMap> buttons;
}
