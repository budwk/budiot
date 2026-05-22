package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_role_menu;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysRoleMenuService;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysRoleMenuServiceImpl extends BaseServiceImpl<Sys_role_menu> implements SysRoleMenuService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysRoleMenuServiceImpl(Dao dao) {
        super(dao);
    }
}
