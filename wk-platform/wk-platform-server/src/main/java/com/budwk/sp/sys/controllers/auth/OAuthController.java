package com.budwk.sp.sys.controllers.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.sys.services.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.model.AuthCallback;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "第三方登录", description = "OAuth2.0 第三方登录接口")
public class OAuthController {
    private final OAuthService oAuthService;

    @GetMapping("/providers")
    @Operation(summary = "获取第三方登录方式")
    public Result<List<Map<String, Object>>> getProviders() {
        return Result.data(oAuthService.getProviders());
    }

    @GetMapping("/authorize/{provider}")
    @Operation(summary = "获取授权地址")
    public Result<Map<String, String>> authorize(
            @Parameter(description = "平台标识: qq/wechat/alipay") @PathVariable String provider,
            @Parameter(description = "前端登录页地址") @RequestParam String frontUrl,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "登录后前端跳转地址") @RequestParam(required = false) String redirect,
            HttpServletRequest request) {
        String requestBaseUrl = getRequestBaseUrl(request);
        String authorizeUrl = oAuthService.getAuthorizeUrl(provider, frontUrl, appId, redirect, requestBaseUrl);
        return Result.data(Map.of("authorizeUrl", authorizeUrl));
    }

    @GetMapping("/callback/{provider}")
    @Operation(summary = "第三方登录回调")
    public void callback(@PathVariable String provider,
                         AuthCallback callback,
                         HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        String targetUrl;
        try {
            targetUrl = oAuthService.handleCallback(provider, callback, getRequestBaseUrl(request), getClientIp(request));
        } catch (Exception e) {
            targetUrl = buildFallbackUrl(request, e.getMessage());
        }
        response.sendRedirect(targetUrl);
    }

    @PostMapping("/login/ticket")
    @Operation(summary = "交换第三方登录票据")
    public Result<Map<String, Object>> exchangeLoginTicket(@RequestBody Map<String, String> body) {
        return Result.data(oAuthService.exchangeLoginTicket(body.get("ticket")));
    }

    @PostMapping("/bind/login")
    @Operation(summary = "第三方账号绑定现有用户")
    @SLog(tag = "用户认证", type = "LOGIN", msg = "第三方账号绑定")
    public Result<Map<String, Object>> bindLogin(@RequestBody Map<String, String> body,
                                    HttpServletRequest request) throws Exception {
        Map<String, Object> loginResult = oAuthService.bindLogin(
                body.get("ticket"),
                body.get("loginname"),
                body.get("password"),
                body.get("rsaKey"),
                body.get("captchaKey"),
                body.get("captchaCode"),
                body.get("tenantName"),
                getClientIp(request)
        );
        return Result.data(loginResult);
    }

    @SaCheckLogin
    @PostMapping("/bind/{provider}")
    @Operation(summary = "绑定当前登录用户")
    public Result<Void> bind(@PathVariable String provider,
                             @RequestBody Map<String, String> body) {
        oAuthService.bindCurrentUser(provider, body.get("ticket"), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/unbind/{provider}")
    @Operation(summary = "解绑当前登录用户")
    public Result<Void> unbind(@PathVariable String provider) {
        oAuthService.unbind(provider, StpUtil.getLoginIdAsString());
        return Result.success();
    }

    private String getRequestBaseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwardedProto) && StringUtils.hasText(forwardedHost)) {
            return forwardedProto + "://" + forwardedHost + "/platform";
        }
        StringBuffer url = request.getRequestURL();
        String requestUri = request.getRequestURI();
        return url.substring(0, url.length() - requestUri.length()) + "/platform";
    }

    private String buildFallbackUrl(HttpServletRequest request, String msg) {
        String referer = request.getHeader("Referer");
        String frontUrl = StringUtils.hasText(referer) ? referer.split("\\?")[0] : "http://127.0.0.1:1818/platform/login";
        String message = StringUtils.hasText(msg) ? msg : "第三方登录失败";
        return frontUrl + "?oauthMsg=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }

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
