package com.budwk.sp.sys.services;

import com.budwk.sp.starter.database.service.BaseService;
import com.budwk.sp.sys.entity.Sys_tenant_package;
import org.nutz.lang.util.NutMap;

import java.util.List;

public interface SysTenantPackageService extends BaseService<Sys_tenant_package> {
    void savePackage(Sys_tenant_package tenantPackage, String[] menuIds, String userId);

    void updatePackage(Sys_tenant_package tenantPackage, String[] menuIds, String userId);

    List<String> getMenuIds(String packageId);

    List<NutMap> getMenuTree();
}
