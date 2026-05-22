package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantContext;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.entity.Sys_menu;
import com.budwk.sp.sys.entity.Sys_role;
import com.budwk.sp.sys.entity.Sys_role_user;
import com.budwk.sp.sys.services.*;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.random.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_role")
public class SysRoleServiceImpl extends BaseServiceImpl<Sys_role> implements SysRoleService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysRoleServiceImpl(Dao dao) {
        super(dao);
    }

    @Autowired
    private SysAppService sysAppService;
    @Autowired
    @Lazy
    private SysUserService sysUserService;
    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysMenuService sysMenuService;

    @Override
    public String getPublicId() {
        AtomicReference<Sys_role> role = new AtomicReference<>();
        WkDaoTenantContext.withoutTenant(()->{
            role.set(this.fetch(Cnd.where("code", "=", "public")));
        });
        return role.get().getId();
    }

    @Override
    public List<String> getPermissionList(Sys_role role) {
        this.fetchLinks(role, "menus", Cnd.where("disabled", "=", false));
        List<String> list = new ArrayList<String>();
        for (Sys_menu menu : role.getMenus()) {
            if (!Strings.isEmpty(menu.getPermission())) {
                list.add(menu.getPermission());
            }
        }
        return list;
    }

    @Override
    public List<Sys_app> getAppList(String userId) {
        Sql sql = Sqls.create("SELECT a.* from sys_app a,sys_role_app b where a.id=b.appId and " +
                " b.roleId in(select c.roleId from sys_role_user c,sys_role d where c.roleId=d.id and c.userId=@userId and d.disabled=@f)  order by a.location");
        sql.params().set("userId", userId);
        sql.params().set("f", false);
        return sysAppService.listEntity(sql);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void clearRole(String roleId) {
        dao().clear("sys_role_app", Cnd.where("roleId", "=", roleId));
        dao().clear("sys_role_menu", Cnd.where("roleId", "=", roleId));
        dao().clear("sys_role_user", Cnd.where("roleId", "=", roleId));
        this.delete(roleId);
        //清除下用户权限缓存
        sysUserService.cacheClear();
    }

    @Override
    public Pagination getUserListPage(String roleId, String username, int pageNo, int pageSize, String pageOrderName, String pageOrderBy) {
        Sql sql = Sqls.create("SELECT a.*,c.name as unitname from sys_user a,sys_role_user b,sys_unit c where a.id=b.userId and a.unitId=c.id and " +
                " b.roleId=@roleId $query $order");
        sql.params().set("roleId", roleId);
        if (Strings.isNotBlank(username)) {
            sql.vars().set("query", " and (a.loginname like '%" + username + "%' or a.username like '%" + username + "%')");
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            sql.vars().set("order", " order by a." + pageOrderName + " " + PageUtil.getOrder(pageOrderBy));
        }
        return sysUserService.listPage(pageNo, pageSize, sql);
    }

    @Override
    public Pagination getSelUserListPage(String roleId, String username, boolean sysadmin, String unitId, int pageNo, int pageSize, String pageOrderName, String pageOrderBy) {
        Sql sql;
        if (Strings.isNotBlank(unitId)) {
            sql
                    = Sqls.create("SELECT " +
                    " a.*,b.name as unitname " +
                    " FROM " +
                    " sys_user a " +
                    " LEFT JOIN sys_unit b ON a.unitId = b.id " +
                    " WHERE " +
                    " a.id NOT IN ( SELECT c.userId FROM sys_role_user c WHERE c.roleId = @roleId ) " +
                    " and (a.unitId=@masterId $s1 ) $s2 $order");
            sql.params().set("roleId", roleId);
            //获取总公司或分公司ID
            String masterId = sysUnitService.getMasterCompanyId(unitId);
            //查询出直属及下属部门用户(不垮分公司)
            sql.params().set("masterId", masterId);
            List<String> unitIds = sysUnitService.getSubUnitIds(masterId);
            if (!Lang.isEmpty(unitIds)) {
                String unitIdsString = unitIds.stream()
                        .map(id -> "'" + id + "'")
                        .collect(Collectors.joining(", "));
                sql.vars().set("s1", " or a.unitId in (" + unitIdsString + ")");
            }
        } else {
            sql
                    = Sqls.create("SELECT " +
                    " a.*,b.name as unitname " +
                    " FROM " +
                    " sys_user a " +
                    " LEFT JOIN sys_unit b ON a.unitId = b.id " +
                    " WHERE " +
                    " a.id NOT IN ( SELECT c.userId FROM sys_role_user c WHERE c.roleId = @roleId ) " +
                    " $s2 $order");
        }

        if (Strings.isNotBlank(username)) {
            sql.vars().set("s2", " and (a.loginname like '%" + username + "%' or a.username like '%" + username + "%')");
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            sql.vars().set("order", " order by a." + pageOrderName + " " + PageUtil.getOrder(pageOrderBy));
        }
        return sysUserService.listPage(pageNo, pageSize, sql);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void doLinkUser(String roleId, String userId, String[] userIds) {
        Sys_role role = this.fetch(roleId);
        for (String id : userIds) {
            Sys_role_user role_user = new Sys_role_user();
            role_user.setTenantId(role != null ? role.getTenantId() : null);
            role_user.setRoleId(roleId);
            role_user.setUserId(id);
            role_user.setCreatedBy(userId);
            this.dao().insert(role_user);
        }
        //清除下用户权限缓存
        sysUserService.cacheClear();
    }

    @Override
    public void doLinkUser(String roleId, String id) {
        this.dao().clear("sys_role_user", Cnd.where("userId", "=", id).and("roleId", "=", roleId));
        //清除下用户权限缓存
        sysUserService.cacheClear();
    }

    @Override
    public List<Sys_menu> getMenusAndDatas(String roleId, String appId) {
        Sql sql = Sqls.create("select distinct a.* from sys_menu a,sys_role_menu b where a.id=b.menuId and a.appId=@appId and " +
                " b.roleId=@roleId and a.disabled=@f order by a.location ASC,a.path asc");
        sql.params().set("roleId", roleId);
        sql.params().set("appId", appId);
        sql.params().set("f", false);
        return sysMenuService.listEntity(sql);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void saveMenu(String roleId, String appId, String[] menuIds) {
        Sys_role role = this.fetch(roleId);
        int roleAppNum = this.dao().count("sys_role_app", Cnd.where("roleId", "=", roleId).and("appId", "=", appId));
        if (roleAppNum <= 0) {
            this.dao().insert("sys_role_app", Chain.make("id", R.UU32()).add("tenantId", role == null ? null : role.getTenantId()).add("appId", appId).add("roleId", roleId));
        }
        this.clear("sys_role_menu", Cnd.where("roleId", "=", roleId).and("appId", "=", appId));
        for (String s : menuIds) {
            this.insert("sys_role_menu", Chain.make("id", R.UU32()).add("tenantId", role == null ? null : role.getTenantId()).add("roleId", roleId).add("appId", appId).add("menuId", s));
            Sys_menu menu = sysMenuService.fetch(s);
            //要把上级菜单插入关联表
            for (int i = 4; i < menu.getPath().length(); i = i + 4) {
                Sys_menu tMenu = sysMenuService.fetch(Cnd.where("path", "=", menu.getPath().substring(0, i)));
                int c = this.count("sys_role_menu", Cnd.where("roleId", "=", roleId).and("appId", "=", appId).and("menuId", "=", tMenu.getId()));
                if (c == 0) {
                    this.insert("sys_role_menu", Chain.make("id", R.UU32()).add("tenantId", role == null ? null : role.getTenantId()).add("roleId", roleId).add("appId", appId).add("menuId", tMenu.getId()));
                }
            }
        }
        //清除下用户权限缓存
        sysUserService.cacheClear();
    }

    private String buildPublicRoleCode(String tenantId) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(tenantId)) {
            return "public";
        }
        return "public:" + tenantId;
    }
}
