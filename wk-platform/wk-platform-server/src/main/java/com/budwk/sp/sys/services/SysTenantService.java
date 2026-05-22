package com.budwk.sp.sys.services;

import com.budwk.sp.starter.database.service.BaseService;
import com.budwk.sp.sys.dto.SysTenantDTO;
import com.budwk.sp.sys.entity.Sys_tenant;

public interface SysTenantService extends BaseService<Sys_tenant> {
    void createTenant(SysTenantDTO dto, String userId);

    void updateTenant(SysTenantDTO dto, String userId);

    void changeDisabled(String id, boolean disabled, String userId);

    void updateExpire(String id, boolean hasExpire, Long expireAt, String userId);

    void deleteTenant(String id);

    Sys_tenant getTenant(String id);

    Sys_tenant getTenantByName(String name);

    void checkTenantAvailable(String tenantId);

    String getTenantLoginNotice(String tenantId);
}
