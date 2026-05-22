package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.services.SysKeyService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysKeyProvider.class)
@Service
public class SysKeyProvider implements ISysKeyProvider {
    
    @Autowired
    private SysKeyService sysKeyService;


    @Override
    public String getAppkey(String appid) {
        return sysKeyService.getAppkey(appid);
    }
}
