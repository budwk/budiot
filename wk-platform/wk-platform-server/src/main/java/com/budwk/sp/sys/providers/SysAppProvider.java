package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.services.SysAppService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysAppProvider.class)
@Service
public class SysAppProvider implements ISysAppProvider {
    
    @Autowired
    private SysAppService sysAppService;

    @Override
    public List<Sys_app> listAll(){
        return sysAppService.listAll();
    }

    @Override
    public List<Sys_app> listEnable(){
        return sysAppService.listEnable();
    }
}
