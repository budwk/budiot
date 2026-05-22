package com.budwk.sp.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "租户到期信息DTO")
public class SysTenantExpireDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "是否过期")
    private boolean hasExpire;

    @Schema(description = "过期时间")
    private Long expireAt;
}
