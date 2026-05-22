package com.budwk.sp.sys.services.impl;

import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.KeyType;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.constant.RedisConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.sys.config.OAuthProperties;
import com.budwk.sp.sys.entity.Sys_tenant;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.entity.Sys_user_oauth;
import com.budwk.sp.sys.services.AuthService;
import com.budwk.sp.sys.services.OAuthService;
import com.budwk.sp.sys.services.SysTenantService;
import com.budwk.sp.sys.services.SysUserOauthService;
import com.budwk.sp.sys.websocket.RedisAuthStateCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthAlipayRequest;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import org.nutz.json.Json;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OAuthServiceImpl implements OAuthService {
    private static final long STATE_TIMEOUT_SECONDS = 10 * 60;
    private static final long TICKET_TIMEOUT_SECONDS = 5 * 60;

    private final OAuthProperties oAuthProperties;
    private final RedisAuthStateCache authStateCache;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SysUserOauthService sysUserOauthService;
    private final AuthService authService;
    private final SysTenantService sysTenantService;
    private final WkCacheService wkCacheService;

    public OAuthServiceImpl(OAuthProperties oAuthProperties,
                            RedisAuthStateCache authStateCache,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper,
                            SysUserOauthService sysUserOauthService,
                            AuthService authService,
                            SysTenantService sysTenantService,
                            WkCacheService wkCacheService) {
        this.oAuthProperties = oAuthProperties;
        this.authStateCache = authStateCache;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.sysUserOauthService = sysUserOauthService;
        this.authService = authService;
        this.sysTenantService = sysTenantService;
        this.wkCacheService = wkCacheService;
    }

    @Override
    public List<Map<String, Object>> getProviders() {
        List<Map<String, Object>> list = new ArrayList<>();
        addProvider(list, "qq", "QQ", oAuthProperties.getQq());
        addProvider(list, "wechat", "微信", oAuthProperties.getWechat());
        addProvider(list, "alipay", "支付宝", oAuthProperties.getAlipay());
        return list;
    }

    @Override
    public String getAuthorizeUrl(String provider, String frontUrl, String appId, String redirect, String requestBaseUrl) throws BaseException {
        ProviderMeta meta = this.getProviderMeta(provider);
        if (!StringUtils.hasText(frontUrl)) {
            throw new BaseException("前端回跳地址不能为空");
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        AuthStateContext context = new AuthStateContext();
        context.setProvider(meta.provider());
        context.setFrontUrl(frontUrl);
        context.setAppId(appId);
        context.setRedirect(redirect);
        this.cacheStateContext(state, context);
        return this.createAuthRequest(meta, requestBaseUrl).authorize(state);
    }

    @Override
    public String handleCallback(String provider, AuthCallback callback, String requestBaseUrl, String ip) throws BaseException {
        ProviderMeta meta = this.getProviderMeta(provider);
        if (!StringUtils.hasText(callback.getState())) {
            throw new BaseException("授权状态已失效，请重新发起授权");
        }
        AuthStateContext stateContext = this.getStateContext(callback.getState());
        if (stateContext == null) {
            throw new BaseException("授权状态已失效，请重新发起授权");
        }
        AuthResponse<AuthUser> authResponse = this.createAuthRequest(meta, requestBaseUrl).login(callback);
        if (authResponse == null || !authResponse.ok() || authResponse.getData() == null) {
            throw new BaseException(authResponse != null ? authResponse.getMsg() : "第三方授权失败");
        }
        AuthProfile profile = this.buildProfile(meta.provider(), authResponse.getData());
        Sys_user_oauth oauth = sysUserOauthService.fetchByProviderAndOpenId(profile.getProvider(), profile.getOpenId());
        if (oauth != null && StringUtils.hasText(oauth.getUserId())) {
            Sys_user user = authService.getUserById(oauth.getUserId());
            Map<String, Object> loginResult = authService.loginSuccess(user, stateContext.getAppId(), ip);
            String loginTicket = UUID.randomUUID().toString().replace("-", "");
            stringRedisTemplate.opsForValue().set(RedisConstant.UCENTER_OAUTH_LOGIN_TICKET + loginTicket, Json.toJson(loginResult),
                    TICKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return buildFrontUrl(stateContext.getFrontUrl(), Map.of(
                    "oauthMode", "login",
                    "ticket", loginTicket,
                    "provider", meta.provider(),
                    "redirect", Strings.sNull(stateContext.getRedirect())
            ));
        }
        BindTicket bindTicket = new BindTicket();
        bindTicket.setProfile(profile);
        bindTicket.setAppId(stateContext.getAppId());
        bindTicket.setFrontUrl(stateContext.getFrontUrl());
        bindTicket.setRedirect(stateContext.getRedirect());
        String ticket = UUID.randomUUID().toString().replace("-", "");
        this.cacheBindTicket(ticket, bindTicket);
        return buildFrontUrl(stateContext.getFrontUrl(), Map.of(
                "oauthMode", "bind",
                "ticket", ticket,
                "provider", meta.provider(),
                "providerText", meta.text(),
                "redirect", Strings.sNull(stateContext.getRedirect())
        ));
    }

    @Override
    public Map<String, Object> exchangeLoginTicket(String ticket) throws BaseException {
        if (!StringUtils.hasText(ticket)) {
            throw new BaseException("登录票据不能为空");
        }
        String key = RedisConstant.UCENTER_OAUTH_LOGIN_TICKET + ticket;
        String loginResult = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(loginResult)) {
            throw new BaseException("登录票据已失效，请重新授权");
        }
        stringRedisTemplate.delete(key);
        return Json.fromJson(Map.class, loginResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindLogin(String ticket, String loginname, String encryptedPassword, String rsaKey,
                                         String captchaKey, String captchaCode, String tenantName, String ip) throws Exception {
        if (!StringUtils.hasText(loginname) || !StringUtils.hasText(encryptedPassword)) {
            throw new BaseException("用户名和密码不能为空");
        }
        BindTicket bindTicket = this.getBindTicket(ticket);
        String password = this.decryptPassword(encryptedPassword, rsaKey);
        Sys_tenant tenant = sysTenantService.getTenantByName(tenantName);
        sysTenantService.checkTenantAvailable(tenant.getId());
        Sys_user user = authService.loginByPassword(loginname, password, captchaKey, captchaCode, tenant.getId());
        this.saveOrUpdateBinding(user, bindTicket.getProfile());
        this.removeBindTicket(ticket);
        return authService.loginSuccess(user, bindTicket.getAppId(), ip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCurrentUser(String provider, String ticket, String userId) throws BaseException {
        BindTicket bindTicket = this.getBindTicket(ticket);
        if (!bindTicket.getProfile().getProvider().equals(normalizeProvider(provider))) {
            throw new BaseException("绑定平台不匹配");
        }
        Sys_user user = authService.getUserById(userId);
        this.saveOrUpdateBinding(user, bindTicket.getProfile());
        this.removeBindTicket(ticket);
    }

    @Override
    public void unbind(String provider, String userId) throws BaseException {
        ProviderMeta meta = this.getProviderMeta(provider);
        Sys_user_oauth oauth = sysUserOauthService.fetchByUserIdAndProvider(userId, meta.provider());
        if (oauth == null) {
            throw new BaseException("未找到绑定记录");
        }
        sysUserOauthService.unbind(userId, meta.provider());
    }

    private void saveOrUpdateBinding(Sys_user user, AuthProfile profile) throws BaseException {
        Sys_user_oauth exists = sysUserOauthService.fetchByProviderAndOpenId(profile.getProvider(), profile.getOpenId());
        if (exists != null && !user.getId().equals(exists.getUserId())) {
            throw new BaseException("该第三方账号已绑定其他用户");
        }
        Sys_user_oauth entity = exists != null ? exists : sysUserOauthService.fetchByUserIdAndProvider(user.getId(), profile.getProvider());
        if (entity == null) {
            entity = new Sys_user_oauth();
            entity.setUserId(user.getId());
            entity.setTenantId(user.getTenantId());
            entity.setProvider(profile.getProvider());
        }
        entity.setUserId(user.getId());
        entity.setTenantId(user.getTenantId());
        entity.setProvider(profile.getProvider());
        entity.setOpenId(profile.getOpenId());
        entity.setUnionId(profile.getUnionId());
        entity.setUsername(profile.getUsername());
        entity.setNickname(profile.getNickname());
        entity.setAvatar(profile.getAvatar());
        entity.setAccessToken(profile.getAccessToken());
        entity.setRefreshToken(profile.getRefreshToken());
        entity.setExpireAt(profile.getExpireAt());
        entity.setRawInfo(profile.getRawInfo());
        if (StringUtils.hasText(entity.getId())) {
            sysUserOauthService.updateIgnoreNull(entity);
        } else {
            sysUserOauthService.insert(entity);
        }
    }

    private AuthProfile buildProfile(String provider, AuthUser authUser) {
        AuthToken token = authUser.getToken();
        AuthProfile profile = new AuthProfile();
        profile.setProvider(provider);
        profile.setOpenId(resolveOpenId(authUser, token));
        profile.setUnionId(token != null ? token.getUnionId() : null);
        profile.setUsername(authUser.getUsername());
        profile.setNickname(StringUtils.hasText(authUser.getNickname()) ? authUser.getNickname() : authUser.getUsername());
        profile.setAvatar(authUser.getAvatar());
        profile.setAccessToken(token != null ? token.getAccessToken() : null);
        profile.setRefreshToken(token != null ? token.getRefreshToken() : null);
        profile.setExpireAt(resolveExpireAt(token));
        profile.setRawInfo(authUser.getRawUserInfo() != null ? authUser.getRawUserInfo().toJSONString() : null);
        return profile;
    }

    private Long resolveExpireAt(AuthToken token) {
        if (token == null || token.getExpireIn() <= 0) {
            return null;
        }
        return System.currentTimeMillis() + token.getExpireIn() * 1000L;
    }

    private String resolveOpenId(AuthUser authUser, AuthToken token) {
        if (token != null) {
            if (StringUtils.hasText(token.getOpenId())) {
                return token.getOpenId();
            }
            if (StringUtils.hasText(token.getUserId())) {
                return token.getUserId();
            }
            if (StringUtils.hasText(token.getUid())) {
                return token.getUid();
            }
        }
        if (StringUtils.hasText(authUser.getUuid())) {
            return authUser.getUuid();
        }
        throw new BaseException("第三方账户缺少唯一标识");
    }

    private String decryptPassword(String encryptedPassword, String rsaKey) throws Exception {
        String rsaValue = Strings.sNull(wkCacheService.getCache(RedisConstant.PRE + "ucenter:rsa:" + rsaKey));
        if (!StringUtils.hasText(rsaValue)) {
            throw new BaseException("网页已过期，请刷新网页");
        }
        Map<String, String> keyMap = Json.fromJson(Map.class, rsaValue);
        RSA rsa = new RSA(keyMap.get("privateKey"), keyMap.get("publicKey"));
        return rsa.decryptStr(encryptedPassword, KeyType.PrivateKey);
    }

    private void addProvider(List<Map<String, Object>> list, String value, String text, OAuthProperties.Provider provider) {
        if (!provider.isEnabled()) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", value);
        map.put("text", text);
        list.add(map);
    }

    private void cacheStateContext(String state, AuthStateContext context) throws BaseException {
        try {
            stringRedisTemplate.opsForValue().set(RedisConstant.UCENTER_OAUTH_STATE_CTX + state,
                    objectMapper.writeValueAsString(context), STATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new BaseException("保存授权上下文失败");
        }
    }

    private AuthStateContext getStateContext(String state) throws BaseException {
        String key = RedisConstant.UCENTER_OAUTH_STATE_CTX + state;
        String value = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, AuthStateContext.class);
        } catch (JsonProcessingException e) {
            throw new BaseException("解析授权上下文失败");
        }
    }

    private void cacheBindTicket(String ticket, BindTicket bindTicket) throws BaseException {
        try {
            stringRedisTemplate.opsForValue().set(RedisConstant.UCENTER_OAUTH_BIND_TICKET + ticket,
                    objectMapper.writeValueAsString(bindTicket), TICKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new BaseException("保存绑定票据失败");
        }
    }

    private BindTicket getBindTicket(String ticket) throws BaseException {
        if (!StringUtils.hasText(ticket)) {
            throw new BaseException("绑定票据不能为空");
        }
        String value = stringRedisTemplate.opsForValue().get(RedisConstant.UCENTER_OAUTH_BIND_TICKET + ticket);
        if (!StringUtils.hasText(value)) {
            throw new BaseException("绑定票据已失效，请重新授权");
        }
        try {
            return objectMapper.readValue(value, BindTicket.class);
        } catch (JsonProcessingException e) {
            throw new BaseException("解析绑定票据失败");
        }
    }

    private void removeBindTicket(String ticket) {
        stringRedisTemplate.delete(RedisConstant.UCENTER_OAUTH_BIND_TICKET + ticket);
    }

    private AuthRequest createAuthRequest(ProviderMeta meta, String requestBaseUrl) {
        AuthConfig config = AuthConfig.builder()
                .clientId(meta.config().getClientId())
                .clientSecret(meta.config().getClientSecret())
                .redirectUri(resolveRedirectUri(meta.provider(), meta.config(), requestBaseUrl))
                .alipayPublicKey(meta.config().getAlipayPublicKey())
                .unionId(meta.config().isUnionId())
                .scopes(CollectionUtils.isEmpty(meta.config().getScopes()) ? null : meta.config().getScopes())
                .build();
        return switch (meta.provider()) {
            case "qq" -> new AuthQqRequest(config, authStateCache);
            case "wechat" -> new AuthWeChatOpenRequest(config, authStateCache);
            case "alipay" -> new AuthAlipayRequest(config, authStateCache);
            default -> throw new BaseException("暂不支持该登录方式");
        };
    }

    private String resolveRedirectUri(String provider, OAuthProperties.Provider providerConfig, String requestBaseUrl) {
        if (StringUtils.hasText(providerConfig.getRedirectUri())) {
            return providerConfig.getRedirectUri();
        }
        return requestBaseUrl + "/oauth/callback/" + provider;
    }

    private ProviderMeta getProviderMeta(String provider) throws BaseException {
        String normalized = normalizeProvider(provider);
        return switch (normalized) {
            case "qq" -> {
                OAuthProperties.Provider config = oAuthProperties.getQq();
                validateProvider(normalized, config);
                yield new ProviderMeta(normalized, "QQ", config);
            }
            case "wechat" -> {
                OAuthProperties.Provider config = oAuthProperties.getWechat();
                validateProvider(normalized, config);
                yield new ProviderMeta(normalized, "微信", config);
            }
            case "alipay" -> {
                OAuthProperties.Provider config = oAuthProperties.getAlipay();
                validateProvider(normalized, config);
                yield new ProviderMeta(normalized, "支付宝", config);
            }
            default -> throw new BaseException("暂不支持该登录方式");
        };
    }

    private void validateProvider(String provider, OAuthProperties.Provider config) throws BaseException {
        if (config == null || !config.isEnabled()) {
            throw new BaseException("未启用 " + provider + " 登录");
        }
        if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getClientSecret())) {
            throw new BaseException(provider + " 登录配置不完整");
        }
    }

    private String normalizeProvider(String provider) {
        return Strings.sNull(provider).trim().toLowerCase();
    }

    private String buildFrontUrl(String frontUrl, Map<String, String> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontUrl);
        query.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                builder.replaceQueryParam(key, value);
            }
        });
        return builder.build(true).toUriString();
    }

    private record ProviderMeta(String provider, String text, OAuthProperties.Provider config) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthStateContext {
        private String provider;
        private String frontUrl;
        private String appId;
        private String redirect;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFrontUrl() {
            return frontUrl;
        }

        public void setFrontUrl(String frontUrl) {
            this.frontUrl = frontUrl;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getRedirect() {
            return redirect;
        }

        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BindTicket {
        private AuthProfile profile;
        private String appId;
        private String frontUrl;
        private String redirect;

        public AuthProfile getProfile() {
            return profile;
        }

        public void setProfile(AuthProfile profile) {
            this.profile = profile;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getFrontUrl() {
            return frontUrl;
        }

        public void setFrontUrl(String frontUrl) {
            this.frontUrl = frontUrl;
        }

        public String getRedirect() {
            return redirect;
        }

        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthProfile {
        private String provider;
        private String openId;
        private String unionId;
        private String username;
        private String nickname;
        private String avatar;
        private String accessToken;
        private String refreshToken;
        private Long expireAt;
        private String rawInfo;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getUnionId() {
            return unionId;
        }

        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public Long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(Long expireAt) {
            this.expireAt = expireAt;
        }

        public String getRawInfo() {
            return rawInfo;
        }

        public void setRawInfo(String rawInfo) {
            this.rawInfo = rawInfo;
        }
    }
}
