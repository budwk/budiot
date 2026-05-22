package com.budwk.sp.sys.controllers.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.constant.RedisConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.entity.Sys_tenant;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.sys.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证控制器
 *
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "登录、登出、验证码等接口")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private AuthValidateService authValidateService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private SysAppService sysAppService;
    @Autowired
    private SysTenantService sysTenantService;
    @Autowired
    private SysUserSecurityService sysUserSecurityService;
    @Autowired
    private WkCacheService wkCacheService;

    /**
     * 获取RSA公钥
     */
    @GetMapping("/rsa")
    @Operation(summary = "获取RSA公钥", description = "获取RSA公钥用于加密密码")
    public Result<Map<String, String>> rsa() throws Exception {
        RSA rsa = new RSA();
        String rsaKey = UUID.randomUUID().toString().replace("-", "");
        String privateKey = rsa.getPrivateKeyBase64();
        String publicKey = rsa.getPublicKeyBase64();

        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("privateKey", privateKey);
        keyMap.put("publicKey", publicKey);

        wkCacheService.setCache(
                RedisConstant.PRE + "ucenter:rsa:" + rsaKey,
                Json.toJson(keyMap),
                3600, TimeUnit.SECONDS
        );

        Map<String, String> result = new HashMap<>();
        result.put("rsaKey", rsaKey);
        result.put("rsaPublicKey", publicKey);
        return Result.data(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "密码登录或短信验证码登录")
    @SLog(tag = "用户认证", type = "LOGIN", msg = "用户登录")
    public Result<?> login(
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "用户名") @RequestParam(required = false) String loginname,
            @Parameter(description = "密码(RSA加密)") @RequestParam(required = false) String password,
            @Parameter(description = "手机号码") @RequestParam(required = false) String mobile,
            @Parameter(description = "登录方式: password/mobile") @RequestParam(required = false) String type,
            @Parameter(description = "验证码Key") @RequestParam(required = false) String captchaKey,
            @Parameter(description = "验证码") @RequestParam(required = false) String captchaCode,
            @Parameter(description = "短信验证码") @RequestParam(required = false) String smscode,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "RSA Key") @RequestParam(required = false) String rsaKey,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            HttpServletRequest request) throws Exception {

        if (!StringUtils.hasText(type)) {
            throw new BaseException("登录方式不能为空");
        }

        // 验证应用
        boolean hasApp = false;
        if (sysAppService != null && StringUtils.hasText(appId)) {
            List<Sys_app> appList = sysAppService.listEnable();
            for (Sys_app app : appList) {
                if (appId.equals(app.getId())) {
                    hasApp = true;
                    break;
                }
            }
        } else {
            hasApp = true;
        }

        if (!hasApp) {
            return Result.error("应用未加载成功，请刷新网页");
        }

        Sys_user user;
        Sys_user_security security = sysUserSecurityService.getWithCache();
        String ip = getClientIp(request);
        Sys_tenant tenant = sysTenantService.getTenantByName(tenantName);
        sysTenantService.checkTenantAvailable(tenant.getId());

        if ("password".equalsIgnoreCase(type)) {
            authService.checkLoginname(loginname, ip,
                    security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                    security != null ? security.getNameRetryNum() : 0,
                    security != null ? security.getNameTimeout() : 0);

            String rsaValue = Strings.sNull(wkCacheService.getCache(RedisConstant.PRE + "ucenter:rsa:" + rsaKey));
            if (!StringUtils.hasText(rsaValue)) {
                throw new BaseException("网页已过期，请刷新网页");
            }

            Map<String, String> keyMap = Json.fromJson(Map.class, rsaValue);
            RSA rsa = new RSA(keyMap.get("privateKey"), keyMap.get("publicKey"));
            String decryptedPassword = rsa.decryptStr(password, KeyType.PrivateKey);

            user = authService.loginByPassword(loginname, decryptedPassword, captchaKey, captchaCode, tenant.getId());
        } else if ("mobile".equalsIgnoreCase(type)) {
            authService.checkMobile(mobile, ip,
                    security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                    security != null ? security.getNameRetryNum() : 0,
                    security != null ? security.getNameTimeout() : 0);

            user = authService.loginByMobile(mobile, smscode, tenant.getId());
        } else {
            throw new BaseException("请求方式不正确");
        }

        return Result.success().addData(authService.loginSuccess(user, appId, ip));
    }

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码")
    public Result<Map<String, Object>> captcha() {
        return authValidateService.getCaptcha();
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/smscode")
    @Operation(summary = "发送短信验证码")
    public Result<Void> smscode(
            @Parameter(description = "手机号码") @RequestParam String mobile,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            HttpServletRequest request) throws BaseException {

        Sys_user_security security = sysUserSecurityService.getWithCache();
        String ip = getClientIp(request);
        Sys_tenant tenant = sysTenantService.getTenantByName(tenantName);
        sysTenantService.checkTenantAvailable(tenant.getId());

        authService.checkMobile(mobile, ip,
                security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                security != null ? security.getNameRetryNum() : 0,
                security != null ? security.getNameTimeout() : 0);

        authValidateService.getSmsCode(mobile, "login", tenant.getId());
        return Result.success();
    }

    /**
     * 检查用户名是否存在
     */
    @PostMapping("/check/loginname")
    @Operation(summary = "检查用户名是否存在")
    public Result<List<Map<String, String>>> checkLoginname(
            @Parameter(description = "用户名") @RequestParam String loginname,
            HttpServletRequest request) throws BaseException {

        Sys_user_security security = sysUserSecurityService.getWithCache();
        String ip = getClientIp(request);

        Sys_user user = authService.getUserByLoginname(loginname, ip,
                security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                security != null ? security.getNameRetryNum() : 0,
                security != null ? security.getNameTimeout() : 0);

        if (user == null) {
            throw new BaseException("用户不存在");
        }

        List<Map<String, String>> typeList = new ArrayList<>();
        if (StringUtils.hasText(user.getMobile())) {
            typeList.add(Map.of("key", "mobile", "val", "手机号码"));
        }
        if (StringUtils.hasText(user.getEmail())) {
            typeList.add(Map.of("key", "email", "val", "电子邮箱"));
        }

        return Result.data(typeList);
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/pwd/sendcode")
    @Operation(summary = "发送重置密码验证码")
    public Result<?> pwdSendCode(
            @Parameter(description = "用户名") @RequestParam String loginname,
            @Parameter(description = "验证方式: mobile/email") @RequestParam String type,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            HttpServletRequest request) throws BaseException {

        Sys_user_security security = sysUserSecurityService.getWithCache();
        String ip = getClientIp(request);

        Sys_user user = authService.getUserByLoginname(loginname, ip,
                security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                security != null ? security.getNameRetryNum() : 0,
                security != null ? security.getNameTimeout() : 0);

        if (user == null) {
            throw new BaseException("用户不存在");
        }

        Sys_tenant tenant = sysTenantService.getTenantByName(tenantName);
        sysTenantService.checkTenantAvailable(tenant.getId());
        String msg;
        if ("mobile".equalsIgnoreCase(type)) {
            authValidateService.getSmsCode(user.getMobile(), "password", tenant.getId());
            msg = "验证码已发送至手机号码，请注意查收";
        } else if ("email".equalsIgnoreCase(type)) {
            authValidateService.getEmailCode("您正在重置密码", loginname, user.getEmail(), tenant.getId());
            msg = "验证码已发送至Email，请注意查收";
        } else {
            throw new BaseException("验证方式不正确");
        }

        return Result.success().addMsg(msg);
    }

    /**
     * 保存重置的新密码
     */
    @PostMapping("/pwd/save")
    @Operation(summary = "保存重置的新密码")
    public Result<Void> pwdSave(
            @Parameter(description = "用户名") @RequestParam String loginname,
            @Parameter(description = "验证方式") @RequestParam String type,
            @Parameter(description = "新密码") @RequestParam String password,
            @Parameter(description = "验证码") @RequestParam String code,
            @Parameter(description = "租户名称") @RequestParam(required = false) String tenantName,
            HttpServletRequest request) throws BaseException {

        Sys_user_security security = sysUserSecurityService.getWithCache();
        String ip = getClientIp(request);
        Sys_tenant tenant = sysTenantService.getTenantByName(tenantName);
        sysTenantService.checkTenantAvailable(tenant.getId());
        Sys_user user = authService.getUserByLoginname(loginname, ip,
                security != null && Boolean.TRUE.equals(security.getNameRetryLock()),
                security != null ? security.getNameRetryNum() : 0,
                security != null ? security.getNameTimeout() : 0);

        if (user == null) {
            throw new BaseException("用户不存在");
        }

        if ("mobile".equalsIgnoreCase(type)) {
            authValidateService.checkSMSCode(user.getMobile(), code, tenant.getId());
        } else if ("email".equalsIgnoreCase(type)) {
            authValidateService.checkEmailCode(loginname, code, tenant.getId());
        } else {
            throw new BaseException("验证方式不正确");
        }

        authService.setPwdByLoginname(loginname, password);
        return Result.success();
    }

    /**
     * 获取登录用户信息
     */
    @GetMapping("/info")
    @SaCheckLogin
    @Operation(summary = "获取登录用户信息")
    public Result<Map<String, Object>> info(HttpServletRequest request) throws BaseException {
        String userId = StpUtil.getLoginIdAsString();
        Sys_user user = authService.getUserById(userId);

        Map<String, Object> menuMap = new HashMap<>();
        String appId = Strings.sBlank(request.getHeader("appId"));
        StpUtil.getSession().set("appId", appId);
        menuMap.put(appId, sysUserService.getMenuList(userId, appId));
        menuMap.put(GlobalConstant.DEFAULT_COMMON_APPID,
                sysUserService.getMenuList(userId, GlobalConstant.DEFAULT_COMMON_APPID));

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("token", StpUtil.getTokenInfo());
        data.put("apps", sysUserService.getAppList(userId));
        data.put("permissions", sysUserService.getPermissionList(userId));
        data.put("menus", menuMap);
        data.put("roles", sysUserService.getRoleList(userId));

        if (sysConfigService != null && appId != null) {
            data.put("conf", sysConfigService.getMapAll(appId));
        }

        return Result.data(data);
    }

    /**
     * 检查密码是否需要修改
     */
    @GetMapping("/checkpwd")
    @SaCheckLogin
    @Operation(summary = "检查密码是否需要修改")
    public Result<?> checkpwd() throws BaseException {
        String userId = StpUtil.getLoginIdAsString();
        Sys_user user = sysUserService.getUserById(userId);

        if (user != null) {
            if (Boolean.TRUE.equals(user.isNeedChangePwd())) {
                return Result.success().addData("您的密码还是初始密码，请修改密码！");
            }
            try {
                sysUserService.checkPwdTimeout(user.getId(), user.getPwdResetAt());
            } catch (BaseException e) {
                return Result.success().addData("您的密码已过期，请及时修改密码！");
            }
        }

        return Result.success();
    }

    /**
     * 设置自定义布局
     */
    @PostMapping("/theme")
    @SaCheckLogin
    @Operation(summary = "设置自定义布局")
    public Result<Void> theme(@Parameter(description = "配置内容") @RequestParam String themeConfig) {
        String userId = StpUtil.getLoginIdAsString();
        sysUserService.setThemeConfig(userId, themeConfig);
        return Result.success();
    }

    /**
     * 获取系统参数
     */
    @GetMapping("/conf")
    @Operation(summary = "获取系统参数")
    public Result<Map<String, Object>> conf(@Parameter(description = "应用ID") @RequestParam String appId) {
        if (sysConfigService != null) {
            return Result.data(sysConfigService.getMapOpened(appId));
        }
        return Result.data(Collections.emptyMap());
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    @Operation(summary = "退出登录")
    @SLog(tag = "用户认证", type = "LOGOUT", msg = "退出登录", saveResult = false)
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(GlobalConstant.HEADER_TOKEN);
        if (StringUtils.hasText(token)) {
            StpUtil.logoutByTokenValue(token);
        } else if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return Result.success();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
