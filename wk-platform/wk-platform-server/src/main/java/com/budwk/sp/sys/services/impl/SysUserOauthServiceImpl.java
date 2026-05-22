package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_user_oauth;
import com.budwk.sp.sys.services.SysUserOauthService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysUserOauthServiceImpl extends BaseServiceImpl<Sys_user_oauth> implements SysUserOauthService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysUserOauthServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public Sys_user_oauth fetchByProviderAndOpenId(String provider, String openId) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(openId)) {
            return null;
        }
        return this.fetch(Cnd.where("provider", "=", provider).and("openId", "=", openId));
    }

    @Override
    public Sys_user_oauth fetchByUserIdAndProvider(String userId, String provider) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(provider)) {
            return null;
        }
        return this.fetch(Cnd.where("userId", "=", userId).and("provider", "=", provider));
    }

    @Override
    public void unbind(String userId, String provider) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(provider)) {
            return;
        }
        this.clear(Cnd.where("userId", "=", userId).and("provider", "=", provider));
    }
}
