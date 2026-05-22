package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_key;
import com.budwk.sp.sys.services.SysKeyService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.random.R;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_key")
public class SysKeyServiceImpl extends BaseServiceImpl<Sys_key> implements SysKeyService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysKeyServiceImpl(Dao dao) {
        super(dao);
    }

    private String getAppid() {
        String appid = R.sg(16).next().replaceAll("_", "z");
        if (this.count(Cnd.where("appid", "=", appid)) > 0) {
            return getAppid();
        }
        return appid;
    }

    @Override
    public void createAppkey(String name, String userId) throws BaseException {
        String appid = getAppid();
        Sys_key sysKey = new Sys_key();
        sysKey.setName(name);
        sysKey.setDisabled(false);
        sysKey.setAppid(appid);
        sysKey.setAppkey(R.sg(30).next().replaceAll("_", "z"));
        sysKey.setCreatedBy(userId);
        sysKey.setUpdatedBy(userId);
        this.insert(sysKey);
    }

    @Override
    public void deleteAppkey(String appid) throws BaseException {
        this.clear(Cnd.where("appid", "=", appid));
        this.cacheClear();
    }

    @Override
    public void updateAppkey(String appid, boolean disabled, String userId) throws BaseException {
        this.update(Chain.make("disabled", disabled).add("updatedBy", userId).add("updatedAt", System.currentTimeMillis()), Cnd.where("appid", "=", appid));
        this.cacheClear();
    }

    @Override
    @Cacheable(key = "#appid+'_getAppkey'")
    public String getAppkey(String appid) {
        Sys_key sysApi = this.fetch(Cnd.where("appid", "=", appid).and("disabled", "=", false));
        if (sysApi != null) {
            return sysApi.getAppkey();
        }
        return "";
    }

    @Override
    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}
