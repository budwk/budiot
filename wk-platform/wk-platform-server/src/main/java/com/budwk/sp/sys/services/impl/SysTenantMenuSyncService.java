package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantContext;
import com.budwk.sp.sys.entity.Sys_menu;
import com.budwk.sp.sys.entity.Sys_role;
import com.budwk.sp.sys.entity.Sys_role_app;
import com.budwk.sp.sys.entity.Sys_role_menu;
import com.budwk.sp.sys.entity.Sys_tenant;
import com.budwk.sp.sys.entity.Sys_tenant_package_menu;
import com.budwk.sp.sys.services.SysUserService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.nutz.lang.random.R;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysTenantMenuSyncService {
    private final Dao dao;
    private final SysUserService sysUserService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysTenantMenuSyncService(Dao dao, SysUserService sysUserService) {
        this.dao = dao;
        this.sysUserService = sysUserService;
    }

    public void syncTenantMenus(String tenantId, String packageId) {
        if (Strings.isBlank(tenantId) || Strings.isBlank(packageId)) {
            return;
        }
        WkDaoTenantContext.withoutTenant(() -> {
            Set<String> allowedMenuIds = resolveAllowedMenuIds(packageId);
            List<Sys_role> roles = dao.query(Sys_role.class, Cnd.where("tenantId", "=", tenantId));
            for (Sys_role role : roles) {
                removeDisallowedMenus(role, allowedMenuIds);
            }
            Sys_role adminRole = dao.fetch(Sys_role.class, Cnd.where("tenantId", "=", tenantId)
                    .and("code", "=", buildAdminRoleCode(tenantId)));
            if (adminRole != null) {
                addMissingMenus(adminRole, allowedMenuIds);
            }
            for (Sys_role role : roles) {
                syncRoleApps(role);
            }
            sysUserService.cacheClear();
        });
    }

    public void syncPackageMenus(String packageId) {
        if (Strings.isBlank(packageId)) {
            return;
        }
        WkDaoTenantContext.withoutTenant(() -> {
            List<Sys_tenant> tenants = dao.query(Sys_tenant.class, Cnd.where("packageId", "=", packageId));
            for (Sys_tenant tenant : tenants) {
                syncTenantMenus(tenant.getId(), packageId);
            }
        });
    }

    private Set<String> resolveAllowedMenuIds(String packageId) {
        Set<String> menuIds = dao.query(Sys_tenant_package_menu.class, Cnd.where("packageId", "=", packageId))
                .stream()
                .map(Sys_tenant_package_menu::getMenuId)
                .filter(Strings::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> effective = new LinkedHashSet<>(menuIds);
        for (String menuId : menuIds) {
            Sys_menu menu = dao.fetch(Sys_menu.class, menuId);
            if (menu == null || Strings.isBlank(menu.getPath())) {
                continue;
            }
            for (int i = 4; i < menu.getPath().length(); i += 4) {
                Sys_menu parent = dao.fetch(Sys_menu.class, Cnd.where("path", "=", menu.getPath().substring(0, i)));
                if (parent != null) {
                    effective.add(parent.getId());
                }
            }
        }
        return effective;
    }

    private void removeDisallowedMenus(Sys_role role, Set<String> allowedMenuIds) {
        List<Sys_role_menu> currentMenus = dao.query(Sys_role_menu.class, Cnd.where("roleId", "=", role.getId()));
        List<String> removableIds = currentMenus.stream()
                .map(Sys_role_menu::getMenuId)
                .filter(Strings::isNotBlank)
                .filter(menuId -> !allowedMenuIds.contains(menuId))
                .collect(Collectors.toList());
        if (!removableIds.isEmpty()) {
            dao.clear("sys_role_menu", Cnd.where("roleId", "=", role.getId()).and("menuId", "in", removableIds));
        }
    }

    private void addMissingMenus(Sys_role role, Set<String> allowedMenuIds) {
        Set<String> currentMenuIds = dao.query(Sys_role_menu.class, Cnd.where("roleId", "=", role.getId()))
                .stream()
                .map(Sys_role_menu::getMenuId)
                .filter(Strings::isNotBlank)
                .collect(Collectors.toSet());
        for (String menuId : allowedMenuIds) {
            if (currentMenuIds.contains(menuId)) {
                continue;
            }
            Sys_menu menu = dao.fetch(Sys_menu.class, menuId);
            if (menu == null) {
                continue;
            }
            dao.insert("sys_role_menu", Chain.make("id", R.UU32())
                    .add("tenantId", role.getTenantId())
                    .add("roleId", role.getId())
                    .add("appId", menu.getAppId())
                    .add("menuId", menu.getId()));
        }
    }

    private void syncRoleApps(Sys_role role) {
        Set<String> menuAppIds = dao.query(Sys_role_menu.class, Cnd.where("roleId", "=", role.getId()))
                .stream()
                .map(Sys_role_menu::getAppId)
                .filter(Strings::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Sys_role_app> currentRoleApps = dao.query(Sys_role_app.class, Cnd.where("roleId", "=", role.getId()));
        for (Sys_role_app roleApp : currentRoleApps) {
            if (!menuAppIds.contains(roleApp.getAppId())) {
                dao.clear("sys_role_app", Cnd.where("roleId", "=", role.getId()).and("appId", "=", roleApp.getAppId()));
            }
        }
        Set<String> existingAppIds = currentRoleApps.stream()
                .map(Sys_role_app::getAppId)
                .filter(Strings::isNotBlank)
                .collect(Collectors.toSet());
        for (String appId : menuAppIds) {
            if (!existingAppIds.contains(appId)) {
                dao.insert("sys_role_app", Chain.make("id", R.UU32())
                        .add("tenantId", role.getTenantId())
                        .add("roleId", role.getId())
                        .add("appId", appId));
            }
        }
    }

    private String buildAdminRoleCode(String tenantId) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(tenantId)) {
            return GlobalConstant.DEFAULT_SYSADMIN_ROLECODE;
        }
        return GlobalConstant.DEFAULT_SYSADMIN_ROLECODE + ":" + tenantId;
    }
}
