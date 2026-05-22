package com.budwk.sp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "文件公开状态DTO")
public class FilePublicFlagDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "是否公开不能为空")
    @Schema(description = "是否公开预览")
    private Boolean publicFlag;
}
