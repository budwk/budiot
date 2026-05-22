package com.budwk.sp.sys.services;

import com.budwk.sp.sys.entity.Sys_group;
import com.budwk.sp.starter.database.service.BaseService;

/**
 * @author wizzer@qq.com
 */
public interface SysGroupService extends BaseService<Sys_group> {
    /**
     * 删除角色组
     * @param groupId
     */
    void clearGroup(String groupId);
}
