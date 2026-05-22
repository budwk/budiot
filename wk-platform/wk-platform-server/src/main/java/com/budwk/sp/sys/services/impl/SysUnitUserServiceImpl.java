package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_unit_user;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysUnitUserService;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysUnitUserServiceImpl extends BaseServiceImpl<Sys_unit_user> implements SysUnitUserService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysUnitUserServiceImpl(Dao dao) {
        super(dao);
    }
}
