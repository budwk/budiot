package com.budwk.sp.sys.services.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.budwk.sp.sys.services.AuthService;
import com.budwk.sp.sys.services.SysTenantService;
import com.budwk.sp.sys.services.SysUserService;
import com.budwk.sp.sys.services.SysUserSecurityService;
import com.budwk.sp.sys.services.AuthValidateService;
import com.budwk.sp.sys.services.SysUnitService;
import com.budwk.sp.starter.common.constant.RedisConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.lang.Times;
import org.nutz.lang.util.NutMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthValidateService authValidateService;
    private final StringRedisTemplate redisTemplate;
    private final SysUserService sysUserService;
    private final SysUserSecurityService sysUserSecurityService;
    private final SysUnitService sysUnitService;
    private final SysTenantService sysTenantService;
    private final ISysMsgProvider sysMsgProvider;

    @Override
    public Sys_user loginByPassword(String loginname, String password, String key, String code, String tenantId) throws BaseException {
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && Boolean.TRUE.equals(security.getCaptchaHasEnabled())) {
            authValidateService.checkCode(key, code);
        }
        return sysUserService.loginByPassword(loginname, password, tenantId);
    }

    @Override
    public Sys_user loginByMobile(String mobile, String code, String tenantId) throws BaseException {
        authValidateService.checkSMSCode(mobile, code, tenantId);
        return sysUserService.loginByMobile(mobile, tenantId);
    }

    @Override
    public java.util.Map<String, Object> loginSuccess(Sys_user user, String appId, String ip) throws BaseException {
        this.checkLoginUser(user);
        StpUtil.login(user.getId());
        StpUtil.checkLogin();
        String thisToken = StpUtil.getTokenValue();

        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && Boolean.TRUE.equals(security.getUserSessionOnlyOne())) {
            List<String> tokens = StpUtil.getTokenValueListByLoginId(user.getId());
            for (String token : tokens) {
                if (!thisToken.equals(token)) {
                    StpUtil.logoutByTokenValue(token);
                }
            }
            sysMsgProvider.wsCheckLogin(user.getId(), thisToken);
        }

        sysUserService.setLoginInfo(user.getId(), ip);

        SaSession session = StpUtil.getSession(true);
        session.set("tenantId", user.getTenantId() != null ? user.getTenantId() : GlobalConstant.TENANT_ID_DEFAULT);
        session.set("loginname", user.getLoginname() != null ? user.getLoginname() : "");
        session.set("username", user.getUsername() != null ? user.getUsername() : "");
        session.set("appId", appId != null ? appId : "");
        session.set("unitId", user.getUnitId() != null ? user.getUnitId() : "");
        session.set("unitPath", user.getUnitPath() != null ? user.getUnitPath() : "");

        if (user.getUnitId() != null) {
            Sys_unit unit = sysUnitService.getMasterCompany(user.getUnitId());
            session.set("masterId", unit != null ? unit.getId() : "");
            session.set("masterPath", unit != null ? unit.getPath() : "");
        } else {
            session.set("masterId", "");
            session.set("masterPath", "");
        }
        return NutMap.NEW()
                .addv("token", StpUtil.getTokenInfo().getTokenValue())
                .addv("tenantId", user.getTenantId() != null ? user.getTenantId() : GlobalConstant.TENANT_ID_DEFAULT)
                .addv("tenantNotice", sysTenantService.getTenantLoginNotice(user.getTenantId()));
    }

    @Override
    public void checkLoginname(String loginname, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException {
        if (nameRetryLock) {
            String key = RedisConstant.PRE + "checkLoginname:" + ip;
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            if (count > nameRetryNum - 1) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long m = ttl != null ? ttl / 60 : 0;
                long s = ttl != null ? ttl % 60 : 0;
                throw new BaseException(String.format("您的IP已被锁定，请%s分钟%s秒后重试", m, s));
            }

            try {
                sysUserService.checkLoginname(loginname);
            } catch (BaseException baseException) {
                if ("用户名不存在".equals(baseException.getMessage())) {
                    redisTemplate.opsForValue().set(key, String.valueOf(++count), nameTimeout, TimeUnit.SECONDS);
                }
                throw baseException;
            }
        } else {
            sysUserService.checkLoginname(loginname);
        }
    }

    @Override
    public void checkMobile(String mobile, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException {
        if (nameRetryLock) {
            String key = RedisConstant.PRE + "checkMobile:" + ip;
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            if (count > nameRetryNum - 1) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long m = ttl != null ? ttl / 60 : 0;
                long s = ttl != null ? ttl % 60 : 0;
                throw new BaseException(String.format("您的IP已被锁定，请%s分钟%s秒后重试", m, s));
            }

            try {
                sysUserService.checkMobile(mobile);
            } catch (BaseException baseException) {
                if ("手机号不存在".equals(baseException.getMessage())) {
                    redisTemplate.opsForValue().set(key, String.valueOf(++count), nameTimeout, TimeUnit.SECONDS);
                }
                throw baseException;
            }
        } else {
            sysUserService.checkMobile(mobile);
        }
    }

    @Override
    public Sys_user getUserByLoginname(String loginname, String ip, boolean nameRetryLock, int nameRetryNum, int nameTimeout) throws BaseException {
        if (nameRetryLock) {
            String key = RedisConstant.PRE + "getUserByLoginname:" + ip;
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            if (count > nameRetryNum - 1) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long m = ttl != null ? ttl / 60 : 0;
                long s = ttl != null ? ttl % 60 : 0;
                throw new BaseException(String.format("您的IP已被锁定，请%s分钟%s秒后重试", m, s));
            }

            try {
                return sysUserService.getUserByLoginname(loginname);
            } catch (BaseException baseException) {
                if ("用户不存在".equals(baseException.getMessage())) {
                    redisTemplate.opsForValue().set(key, String.valueOf(++count), nameTimeout, TimeUnit.SECONDS);
                }
                throw baseException;
            }
        } else {
            return sysUserService.getUserByLoginname(loginname);
        }
    }

    @Override
    public Sys_user getUserById(String id) throws BaseException {
        return sysUserService.getUserById(id);
    }

    @Override
    public void setPwdByLoginname(String loginname, String password) throws BaseException {
        sysUserService.setPwdByLoginname(loginname, password);
    }

    @Override
    public void setPwdById(String id, String password) throws BaseException {
        sysUserService.setPwdById(id, password);
    }

    private void checkLoginUser(Sys_user user) throws BaseException {
        if (user == null) {
            throw new BaseException("用户登录失败");
        }
        if (user.isDisabled()) {
            throw new BaseException("用户被禁用");
        }
        sysTenantService.checkTenantAvailable(user.getTenantId());
        if (user.isDisabledLogin() && user.getDisabledLoginAt() != null) {
            Sys_user_security security = sysUserSecurityService.getWithCache();
            if (security != null && security.getHasEnabled() && security.getPwdRetryTime() > 0) {
                if (System.currentTimeMillis() > user.getDisabledLoginAt() + (security.getPwdRetryTime() * 1000L)) {
                    redisTemplate.delete(GlobalConstant.CACHE_PREFIX + user.getId() + ":pwdretrynum");
                    sysUserService.update(Chain.make("disabledLogin", false).add("disabledLoginAt", null),
                            Cnd.where("id", "=", user.getId()));
                } else {
                    throw new BaseException("禁止登录，解锁时间：" +
                            Times.format("MM月dd HH:mm:ss", new Date(user.getDisabledLoginAt() + (security.getPwdRetryTime() * 1000L))));
                }
            }
        }
    }
}
