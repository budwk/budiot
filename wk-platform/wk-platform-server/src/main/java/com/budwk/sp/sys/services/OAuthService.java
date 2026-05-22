package com.budwk.sp.sys.services;

import com.budwk.sp.starter.common.exception.BaseException;
import me.zhyd.oauth.model.AuthCallback;

import java.util.List;
import java.util.Map;

public interface OAuthService {
    List<Map<String, Object>> getProviders();

    String getAuthorizeUrl(String provider, String frontUrl, String appId, String redirect, String requestBaseUrl) throws BaseException;

    String handleCallback(String provider, AuthCallback callback, String requestBaseUrl, String ip) throws BaseException;

    Map<String, Object> exchangeLoginTicket(String ticket) throws BaseException;

    Map<String, Object> bindLogin(String ticket, String loginname, String encryptedPassword, String rsaKey,
                                  String captchaKey, String captchaCode, String tenantName, String ip) throws Exception;

    void bindCurrentUser(String provider, String ticket, String userId) throws BaseException;

    void unbind(String provider, String userId) throws BaseException;
}
