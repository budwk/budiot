package com.budwk.sp.device.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设备分类DTO")
public class DeviceCategoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    private String id;
    private String tenantId;
    private String parentId;
    private String path;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String name;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 32, message = "分类编码长度不能超过32")
    private String code;

    private boolean disabled;
    private Integer location;
    private boolean hasChildren;
}
