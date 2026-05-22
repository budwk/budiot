package com.budwk.sp.sys.services;

import com.budwk.sp.starter.database.service.BaseService;
import com.budwk.sp.sys.entity.Sys_user_oauth;

public interface SysUserOauthService extends BaseService<Sys_user_oauth> {
    Sys_user_oauth fetchByProviderAndOpenId(String provider, String openId);

    Sys_user_oauth fetchByUserIdAndProvider(String userId, String provider);

    void unbind(String userId, String provider);
}
