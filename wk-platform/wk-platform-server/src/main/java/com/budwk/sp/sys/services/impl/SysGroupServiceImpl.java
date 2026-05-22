package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_group;
import com.budwk.sp.sys.entity.Sys_role;
import com.budwk.sp.sys.services.SysGroupService;
import com.budwk.sp.sys.services.SysRoleService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysGroupServiceImpl extends BaseServiceImpl<Sys_group> implements SysGroupService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysGroupServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private SysRoleService sysRoleService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void clearGroup(String groupId) {
        List<Sys_role> roleList = sysRoleService.query(Cnd.where("groupId", "=", groupId));
        for (Sys_role role : roleList) {
            sysRoleService.clearRole(role.getId());
        }
        this.delete(groupId);
    }
}
