package com.budwk.sp.sys.dto;

import com.budwk.sp.starter.common.valid.group.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
/**
 * 用户第三方账号 DTO
 */
@Data
@Schema(description = "用户第三方账号绑定传输对象")
public class SysUserOauthDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = Update.class, message = "修改时ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "平台标识")
    private String provider;

    @Schema(description = "第三方唯一标识")
    private String openId;

    @Schema(description = "UnionId")
    private String unionId;

    @Schema(description = "第三方用户名")
    private String username;

    @Schema(description = "第三方昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "access_token")
    private String accessToken;

    @Schema(description = "refresh_token")
    private String refreshToken;

    @Schema(description = "过期时间")
    private Long expireAt;

    @Schema(description = "原始数据")
    private String rawInfo;
}
