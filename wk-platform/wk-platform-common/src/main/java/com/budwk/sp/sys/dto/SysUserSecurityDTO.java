package com.budwk.sp.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
/**
 * 账户安全配置 DTO
 *
 * @author wizzer@qq.com
 */
@Data
@Schema(description = "账户安全配置传输对象")
public class SysUserSecurityDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID不能为空")
    @Schema(description = "ID")
    private String id;

    @Schema(description = "是否启用")
    private Boolean hasEnabled;

    @Schema(description = "密码最小长度")
    private Integer pwdLengthMin;

    @Schema(description = "密码最大长度")
    private Integer pwdLengthMax;

    @Schema(description = "密码字符要求")
    private Integer pwdCharMust;

    @Schema(description = "不能包含的用户信息")
    private String pwdCharNot;

    @Schema(description = "密码重复性检查")
    private Boolean pwdRepeatCheck;

    @Schema(description = "重复性检查记录数")
    private Integer pwdRepeatNum;

    @Schema(description = "超重试次数锁定账号")
    private Boolean pwdRetryLock;

    @Schema(description = "密码错误最大重试次数")
    private Integer pwdRetryNum;

    @Schema(description = "超重试次数处理方式")
    private Integer pwdRetryAction;

    @Schema(description = "密码错误禁止登录时长")
    private Integer pwdRetryTime;

    @Schema(description = "指定密码过期时间(天)")
    private Integer pwdTimeoutDay;

    @Schema(description = "重置密码后需修改密码")
    private Boolean pwdResetChange;

    @Schema(description = "用户名/手机号输错锁定")
    private Boolean nameRetryLock;

    @Schema(description = "用户名/手机号输错次数")
    private Integer nameRetryNum;

    @Schema(description = "输错IP锁定时间(秒)")
    private Integer nameTimeout;

    @Schema(description = "用户单一登录")
    private Boolean userSessionOnlyOne;

    @Schema(description = "是否启用登录验证码")
    private Boolean captchaHasEnabled;

    @Schema(description = "验证码类型")
    private Integer captchaType;
}
