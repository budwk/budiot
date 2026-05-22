package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.entity.Sys_menu;
import com.budwk.sp.sys.entity.Sys_tenant_package;
import com.budwk.sp.sys.entity.Sys_tenant_package_menu;
import com.budwk.sp.sys.services.SysTenantPackageService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysTenantPackageServiceImpl extends BaseServiceImpl<Sys_tenant_package> implements SysTenantPackageService {
    private final SysTenantMenuSyncService sysTenantMenuSyncService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysTenantPackageServiceImpl(Dao dao, SysTenantMenuSyncService sysTenantMenuSyncService) {
        super(dao);
        this.sysTenantMenuSyncService = sysTenantMenuSyncService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void savePackage(Sys_tenant_package tenantPackage, String[] menuIds, String userId) {
        tenantPackage.setCreatedBy(userId);
        this.insert(tenantPackage);
        savePackageMenus(tenantPackage.getId(), menuIds, userId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void updatePackage(Sys_tenant_package tenantPackage, String[] menuIds, String userId) {
        tenantPackage.setUpdatedBy(userId);
        this.updateIgnoreNull(tenantPackage);
        this.clear("sys_tenant_package_menu", Cnd.where("packageId", "=", tenantPackage.getId()));
        savePackageMenus(tenantPackage.getId(), menuIds, userId);
        sysTenantMenuSyncService.syncPackageMenus(tenantPackage.getId());
    }

    @Override
    public List<String> getMenuIds(String packageId) {
        return this.dao().query(Sys_tenant_package_menu.class, Cnd.where("packageId", "=", packageId))
                .stream()
                .map(Sys_tenant_package_menu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    public List<NutMap> getMenuTree() {
        List<Sys_app> apps = this.dao().query(Sys_app.class, Cnd.where("disabled", "=", false).and("id", "<>", "COMMON").asc("location"));
        List<Sys_menu> menus = filterAssignableMenus(this.dao().query(Sys_menu.class, Cnd.where("disabled", "=", false).and("appId", "<>", "COMMON").asc("location").asc("path")));
        List<NutMap> tree = new ArrayList<>();
        for (Sys_app app : apps) {
            List<NutMap> children = buildAppMenuTree(app.getId(), "", menus);
            if (children.isEmpty()) {
                continue;
            }
            NutMap root = NutMap.NEW();
            root.put("id", "app-" + app.getId());
            root.put("label", app.getName());
            root.put("disabled", true);
            root.put("children", children);
            tree.add(root);
        }
        return tree;
    }

    private List<NutMap> buildAppMenuTree(String appId, String parentId, List<Sys_menu> allMenus) {
        List<NutMap> tree = new ArrayList<>();
        for (Sys_menu menu : allMenus) {
            if (!appId.equals(menu.getAppId()) || !Strings.sNull(menu.getParentId()).equals(parentId)) {
                continue;
            }
            NutMap map = NutMap.NEW();
            map.put("id", menu.getId());
            map.put("label", "data".equals(menu.getType()) ? "[权限] " + menu.getName() : menu.getName());
            map.put("menuType", menu.getType());
            List<NutMap> children = buildAppMenuTree(appId, menu.getId(), allMenus);
            if (!children.isEmpty()) {
                map.put("children", children);
            }
            tree.add(map);
        }
        return tree;
    }

    private void savePackageMenus(String packageId, String[] menuIds, String userId) {
        if (menuIds == null) {
            return;
        }
        List<Sys_menu> allMenus = this.dao().query(Sys_menu.class, Cnd.where("disabled", "=", false).and("appId", "<>", "COMMON").asc("location").asc("path"));
        Set<String> allowedMenuIds = filterAssignableMenus(allMenus).stream().map(Sys_menu::getId).collect(Collectors.toSet());
        for (String menuId : menuIds) {
            if (Strings.isBlank(menuId) || !allowedMenuIds.contains(menuId)) {
                continue;
            }
            Sys_menu menu = this.dao().fetch(Sys_menu.class, menuId);
            if (menu == null || Strings.isBlank(menu.getAppId()) || "COMMON".equals(menu.getAppId())) {
                continue;
            }
            Sys_tenant_package_menu packageMenu = new Sys_tenant_package_menu();
            packageMenu.setPackageId(packageId);
            packageMenu.setAppId(menu.getAppId());
            packageMenu.setMenuId(menuId);
            packageMenu.setCreatedBy(userId);
            this.dao().insert(packageMenu);
        }
    }

    private List<Sys_menu> filterAssignableMenus(List<Sys_menu> menus) {
        Set<String> forbiddenPathPrefixes = new LinkedHashSet<>();
        for (Sys_menu menu : menus) {
            if (isForbiddenMenu(menu)) {
                forbiddenPathPrefixes.add(menu.getPath());
            }
        }
        if (forbiddenPathPrefixes.isEmpty()) {
            return menus;
        }
        return menus.stream()
                .filter(menu -> forbiddenPathPrefixes.stream().noneMatch(prefix -> Strings.isNotBlank(menu.getPath()) && menu.getPath().startsWith(prefix)))
                .collect(Collectors.toList());
    }

    // 禁止一些系统菜单分配给租户
    private boolean isForbiddenMenu(Sys_menu menu) {
        String permission = Strings.sNull(menu.getPermission());
        String name = Strings.sNull(menu.getName());
        return permission.startsWith("sys.manage.tenant")
                || permission.startsWith("sys.manage.app")
                || permission.startsWith("sys.manage.menu")
                || permission.startsWith("sys.config")
                || permission.startsWith("sys.manage.role.system")
                || "租户管理".equals(name)
                || "应用管理".equals(name)
                || "菜单管理".equals(name);
    }
}
