package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.services.SysUnitService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysUnitProvider.class)
@Service
public class SysUnitProvider implements ISysUnitProvider {
    
    @Autowired
    private SysUnitService sysUnitService;

    @Override
    public String getMasterCompanyId(String unitId) {
        return sysUnitService.getMasterCompanyId(unitId);
    }

    @Override
    public String getMasterCompanyPath(String unitId) {
        return sysUnitService.getMasterCompanyPath(unitId);
    }

    @Override
    public Sys_unit getMasterCompany(String unitId) {
        return sysUnitService.getMasterCompany(unitId);
    }
}
