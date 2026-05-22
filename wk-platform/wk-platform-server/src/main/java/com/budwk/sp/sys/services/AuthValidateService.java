package com.budwk.sp.sys.services;

import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.result.Result;

import java.util.Map;

/**
 * 验证码服务接口
 *
 * @author wizzer@qq.com
 */
public interface AuthValidateService {

    /**
     * 获取图形验证码
     */
    Result<Map<String, Object>> getCaptcha();

    /**
     * 发送短信验证码
     *
     * @param mobile 手机号码
     * @param type   验证码类型(register-注册验证码/login-登录验证码/password-找回密码)
     */
    void getSmsCode(String mobile, String type, String tenantId) throws BaseException;

    /**
     * 发送邮件验证码
     */
    void getEmailCode(String subject, String loginname, String email, String tenantId) throws BaseException;

    /**
     * 核验图形验证码
     */
    void checkCode(String key, String code) throws BaseException;

    /**
     * 核验短信验证码
     */
    void checkSMSCode(String mobile, String code,String tenantId) throws BaseException;

    /**
     * 核验邮件验证码
     */
    void checkEmailCode(String loginname, String code,String tenantId) throws BaseException;
}
