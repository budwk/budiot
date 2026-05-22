package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.services.SysConfigService;
import org.apache.dubbo.config.annotation.DubboService;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysConfigProvider.class)
@Service
public class SysConfigProvider implements ISysConfigProvider {
    
    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public NutMap getMapAll(String appId) {
        return sysConfigService.getMapAll(appId);
    }

    @Override
    public NutMap getMapOpened(String appId) {
        return sysConfigService.getMapOpened(appId);
    }

    @Override
    public String getString(String appId, String key) {
        return sysConfigService.getString(appId,key);
    }

    @Override
    public boolean getBoolean(String appId, String key) {
        return sysConfigService.getBoolean(appId,key);
    }
}
