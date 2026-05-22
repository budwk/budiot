package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_config;
import com.budwk.sp.sys.enums.SysConfigType;
import com.budwk.sp.sys.services.SysAppService;
import com.budwk.sp.sys.services.SysConfigService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.lang.util.NutMap;
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
@CacheConfig(cacheNames = "sys_config")
public class SysConfigServiceImpl extends BaseServiceImpl<Sys_config> implements SysConfigService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysConfigServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private SysAppService sysAppService;

    @Override
    @Cacheable(key = "#appId+'_getMapAll'")
    public NutMap getMapAll(String appId) {
        NutMap nutMap = NutMap.NEW();
        List<Sys_config> commonConfig = this.query(Cnd.where("appId", "=", GlobalConstant.DEFAULT_COMMON_APPID));
        for (Sys_config config : commonConfig) {
            if (SysConfigType.BOOL == config.getType()) {
                nutMap.put(config.getConfigKey(), Boolean.valueOf(config.getConfigValue()));
            } else {
                nutMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        List<Sys_config> appConfig = this.query(Cnd.where("appId", "=", appId));
        for (Sys_config config : appConfig) {
            if (SysConfigType.BOOL == config.getType()) {
                nutMap.put(config.getConfigKey(), Boolean.valueOf(config.getConfigValue()));
            } else {
                nutMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return nutMap;
    }

    @Override
    @Cacheable(key = "#appId+'_getMapOpened'")
    public NutMap getMapOpened(String appId) {
        NutMap nutMap = NutMap.NEW();
        List<Sys_config> commonConfig = this.query(Cnd.where("appId", "=", GlobalConstant.DEFAULT_COMMON_APPID)
                .and("opened", "=", true));
        for (Sys_config config : commonConfig) {
            if (SysConfigType.BOOL == config.getType()) {
                nutMap.put(config.getConfigKey(), Boolean.valueOf(config.getConfigValue()));
            } else {
                nutMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        List<Sys_config> appConfig = this.query(Cnd.where("appId", "=", appId)
                .and("opened", "=", true));
        for (Sys_config config : appConfig) {
            if (SysConfigType.BOOL == config.getType()) {
                nutMap.put(config.getConfigKey(), Boolean.valueOf(config.getConfigValue()));
            } else {
                nutMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return nutMap;
    }

    @Override
    public String getString(String appId, String key) {
        return getMapAll(appId).getString(key, "");
    }

    @Override
    public boolean getBoolean(String appId, String key) {
        return getMapAll(appId).getBoolean(key, false);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}
