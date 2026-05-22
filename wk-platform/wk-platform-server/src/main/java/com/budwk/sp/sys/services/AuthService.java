package com.budwk.sp.sys.services;

import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.starter.common.exception.BaseException;

/**
 * 认证服务接口
 *
 * @author wizzer@qq.com
 */
public interface AuthService {

    /**
     * 通过用户密码登录
     *
     * @param loginname 用户名
     * @param password  密码
     * @param key       验证码key
     * @param code      验证码
     * @return 用户信息
     */
    Sys_user loginByPassword(String loginname, String password, String key, String code, String tenantId) throws BaseException;

    /**
     * 通过短信验证码登录
     *
     * @param mobile 手机号码
     * @param code   短信验证码
     * @return 用户信息
     */
    Sys_user loginByMobile(String mobile, String code, String tenantId) throws BaseException;

    /**
     * 登录成功后写入 Sa-Token 会话并返回 token
     */
    java.util.Map<String, Object> loginSuccess(Sys_user user, String appId, String ip) throws BaseException;

    /**
     * 检查用户是否存在（用户名）
     */
    void checkLoginname(String loginname, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException;

    /**
     * 检查用户是否存在（手机号）
     */
    void checkMobile(String mobile, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException;

    /**
     * 通过用户名获取用户信息
     */
    Sys_user getUserByLoginname(String loginname, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException;

    /**
     * 通过用户ID获取用户信息
     */
    Sys_user getUserById(String id) throws BaseException;

    /**
     * 通过用户名重置密码
     */
    void setPwdByLoginname(String loginname, String password) throws BaseException;

    /**
     * 通过用户ID重置密码
     */
    void setPwdById(String id, String password) throws BaseException;
}
