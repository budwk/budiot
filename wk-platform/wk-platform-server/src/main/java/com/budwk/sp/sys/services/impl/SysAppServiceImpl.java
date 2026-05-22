package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.services.SysAppService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_app")
public class SysAppServiceImpl extends BaseServiceImpl<Sys_app> implements SysAppService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    //@Qualifier("userDao")
    public SysAppServiceImpl(Dao dao) {
        super(dao);
    }

    @Cacheable(key = "'listAll'")
    public List<Sys_app> listAll() {
        return this.query("^(id|name|path|disabled)$", Cnd.NEW().asc("location"));
    }

    @Cacheable(key = "'listEnable'")
    public List<Sys_app> listEnable() {
        return this.query("^(id|name|path|disabled)$", Cnd.where("disabled", "=", false).asc("location"));
    }

    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}
