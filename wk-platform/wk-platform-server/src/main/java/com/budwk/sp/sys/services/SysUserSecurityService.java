package com.budwk.sp.sys.services;


import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.starter.database.service.BaseService;

/**
 * @author wizzer@qq.com
 */
public interface SysUserSecurityService extends BaseService<Sys_user_security> {
    /**
     * 更新或修改账户安全配置
     * @param security 对象
     */
    void insertOrUpdate(Sys_user_security security);

    /**
     * 获取账户安全配置
     * @return
     */
    Sys_user_security getWithCache();

    /**
     * 清除缓存
     */
    void cacheClear();
}
