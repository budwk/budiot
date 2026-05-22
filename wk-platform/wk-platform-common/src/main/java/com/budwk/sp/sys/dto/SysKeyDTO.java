package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
/**
 * 系统密钥 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "系统密钥传输对象")
public class SysKeyDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Size(max = 32, message = "AppID长度不能超过32")
    @Schema(description = "AppID")
    private String appid;

    @Size(max = 32, message = "AppKey长度不能超过32")
    @Schema(description = "AppKey")
    private String appkey;

    @Size(max = 32, message = "名称长度不能超过32")
    @Schema(description = "名称")
    private String name;

    @Schema(description = "是否禁用", defaultValue = "false")
    private boolean disabled;
}
