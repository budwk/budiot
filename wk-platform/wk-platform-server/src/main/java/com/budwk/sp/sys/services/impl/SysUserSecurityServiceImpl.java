package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysUserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_user_security")
@Slf4j
public class SysUserSecurityServiceImpl extends BaseServiceImpl<Sys_user_security> implements SysUserSecurityService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysUserSecurityServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public void insertOrUpdate(Sys_user_security security) {
        int num = this.count(Cnd.where("id", "=", "MAIN"));
        security.setId("MAIN");
        if (num > 0) {
            this.updateIgnoreNull(security);
        } else {
            this.insert(security);
        }
        this.cacheClear();
    }

    @Override
    @Cacheable
    public Sys_user_security getWithCache() {
        return this.fetch("MAIN");
    }

    @Override
    @CacheEvict(allEntries = true)
    public void cacheClear() {

    }
}
