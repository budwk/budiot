package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_role_app;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysRoleAppService;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysRoleAppServiceImpl extends BaseServiceImpl<Sys_role_app> implements SysRoleAppService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysRoleAppServiceImpl(Dao dao) {
        super(dao);
    }
}
