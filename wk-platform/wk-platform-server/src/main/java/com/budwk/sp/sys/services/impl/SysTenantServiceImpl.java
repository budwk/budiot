package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantContext;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.dto.SysTenantDTO;
import com.budwk.sp.sys.entity.*;
import com.budwk.sp.sys.enums.SysUnitType;
import com.budwk.sp.sys.services.SysTenantPackageService;
import com.budwk.sp.sys.services.SysTenantService;
import com.budwk.sp.sys.services.SysUserService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.nutz.lang.random.R;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysTenantServiceImpl extends BaseServiceImpl<Sys_tenant> implements SysTenantService {
    private final SysTenantPackageService sysTenantPackageService;
    private final SysUserService sysUserService;
    private final SysTenantMenuSyncService sysTenantMenuSyncService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysTenantServiceImpl(Dao dao, @Lazy SysTenantPackageService sysTenantPackageService,
                                @Lazy SysUserService sysUserService, SysTenantMenuSyncService sysTenantMenuSyncService) {
        super(dao);
        this.sysTenantPackageService = sysTenantPackageService;
        this.sysUserService = sysUserService;
        this.sysTenantMenuSyncService = sysTenantMenuSyncService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void createTenant(SysTenantDTO dto, String userId) {
        WkDaoTenantContext.setIgnoreTenant(true);
        try {
            Sys_tenant_package tenantPackage = getEnabledPackage(dto.getPackageId());
            checkLoginnameUnique(dto.getAdminLoginname(), null);
            if (Strings.isBlank(dto.getAdminPassword())) {
                throw new BaseException("管理员密码不能为空");
            }
            Sys_tenant tenant = new Sys_tenant();
            tenant.setName(Strings.sNull(dto.getName()).trim());
            tenant.setPackageId(dto.getPackageId());
            tenant.setAdminLoginname(Strings.sNull(dto.getAdminLoginname()).trim());
            tenant.setHasExpire(dto.isHasExpire());
            tenant.setExpireAt(dto.isHasExpire() ? dto.getExpireAt() : null);
            tenant.setDisabled(dto.isDisabled());
            tenant.setCreatedBy(userId);
            this.insert(tenant);
            provisionTenant(tenant, tenantPackage, dto.getAdminPassword(), userId);
        } finally {
            WkDaoTenantContext.clear();
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void updateTenant(SysTenantDTO dto, String userId) {
        WkDaoTenantContext.setIgnoreTenant(true);
        try {
            Sys_tenant tenant = getTenant(dto.getId());
            ensureEditable(tenant.getId());
            Sys_tenant_package tenantPackage = getEnabledPackage(dto.getPackageId());
            Sys_user adminUser = getTenantAdminUser(tenant);
            checkLoginnameUnique(dto.getAdminLoginname(), adminUser.getId());
            String oldName = tenant.getName();

            tenant.setName(Strings.sNull(dto.getName()).trim());
            tenant.setPackageId(dto.getPackageId());
            tenant.setAdminLoginname(Strings.sNull(dto.getAdminLoginname()).trim());
            tenant.setHasExpire(dto.isHasExpire());
            tenant.setExpireAt(dto.isHasExpire() ? dto.getExpireAt() : null);
            tenant.setUpdatedBy(userId);
            this.updateIgnoreNull(tenant);

            if (!Strings.equals(oldName, tenant.getName())) {
                Sys_unit company = getTenantCompany(tenant.getId());
                company.setName(tenant.getName());
                company.setAliasName(tenant.getName());
                company.setUpdatedBy(userId);
                this.dao().updateIgnoreNull(company);
            }

            adminUser.setLoginname(tenant.getAdminLoginname());
            adminUser.setUpdatedBy(userId);
            this.dao().updateIgnoreNull(adminUser);
            if (Strings.isNotBlank(dto.getAdminPassword())) {
                sysUserService.setPwdById(adminUser.getId(), dto.getAdminPassword());
            }

            applyPackageMenus(tenant.getId(), tenantPackage);
        } finally {
            WkDaoTenantContext.clear();
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void changeDisabled(String id, boolean disabled, String userId) {
        ensureEditable(id);
        this.update(Chain.make("disabled", disabled).add("updatedBy", userId), Cnd.where("id", "=", id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void updateExpire(String id, boolean hasExpire, Long expireAt, String userId) {
        ensureEditable(id);
        this.update(Chain.make("hasExpire", hasExpire).add("expireAt", hasExpire ? expireAt : null).add("updatedBy", userId), Cnd.where("id", "=", id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void deleteTenant(String id) {
        WkDaoTenantContext.setIgnoreTenant(true);
        try {
            ensureEditable(id);
            List<Sys_user> users = this.dao().query(Sys_user.class, Cnd.where("tenantId", "=", id));
            List<String> userIds = new ArrayList<>();
            for (Sys_user user : users) {
                userIds.add(user.getId());
            }
            if (!userIds.isEmpty()) {
                this.dao().clear("sys_user_pwd", Cnd.where("userId", "in", userIds));
                this.dao().clear("sys_user_oauth", Cnd.where("userId", "in", userIds));
            }
            this.dao().clear("sys_role_user", Cnd.where("tenantId", "=", id));
            this.dao().clear("sys_unit_user", Cnd.where("tenantId", "=", id));
            this.dao().clear("sys_role_menu", Cnd.where("tenantId", "=", id));
            this.dao().clear("sys_role_app", Cnd.where("tenantId", "=", id));
            this.dao().clear(Sys_group.class, Cnd.where("tenantId", "=", id));
            this.dao().clear(Sys_role.class, Cnd.where("tenantId", "=", id));
            this.dao().clear(Sys_user.class, Cnd.where("tenantId", "=", id));
            this.dao().clear(Sys_unit.class, Cnd.where("tenantId", "=", id));
            this.delete(id);
        } finally {
            WkDaoTenantContext.clear();
        }
    }

    @Override
    public Sys_tenant getTenant(String id) {
        Sys_tenant tenant = this.fetch(id);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        return tenant;
    }

    @Override
    public Sys_tenant getTenantByName(String name) {
        String tenantName = Strings.sBlank(Strings.trim(name), "平台默认");
        Sys_tenant tenant = this.fetch(Cnd.where("name", "=", tenantName));
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        return tenant;
    }

    @Override
    public void checkTenantAvailable(String tenantId) {
        if (Strings.isBlank(tenantId)) {
            return;
        }
        Sys_tenant tenant = this.fetch(tenantId);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        if (tenant.isDisabled()) {
            throw new BaseException("租户已被禁用");
        }
        if (tenant.isHasExpire() && tenant.getExpireAt() != null && tenant.getExpireAt() < System.currentTimeMillis()) {
            throw new BaseException("租户已过期");
        }
    }

    @Override
    public String getTenantLoginNotice(String tenantId) {
        if (Strings.isBlank(tenantId)) {
            return null;
        }
        Sys_tenant tenant = this.fetch(tenantId);
        if (tenant == null || !tenant.isHasExpire() || tenant.getExpireAt() == null) {
            return null;
        }
        long remain = tenant.getExpireAt() - System.currentTimeMillis();
        if (remain <= 0) {
            return null;
        }
        long sevenDays = 7L * 24 * 60 * 60 * 1000;
        if (remain > sevenDays) {
            return null;
        }
        long oneDay = 24L * 60 * 60 * 1000;
        long days = Math.max(1, (remain + oneDay - 1) / oneDay);
        return "当前租户将于" + days + "天后到期，请联系平台管理员续期";
    }

    private void provisionTenant(Sys_tenant tenant, Sys_tenant_package tenantPackage, String adminPassword, String userId) {
        Sys_unit company = new Sys_unit();
        company.setTenantId(tenant.getId());
        company.setParentId("");
        company.setPath(getSubPath("sys_unit", "path", ""));
        company.setType(SysUnitType.COMPANY);
        company.setName(tenant.getName());
        company.setAliasName(tenant.getName());
        company.setDisabled(tenant.isDisabled());
        company.setHasChildren(false);
        company.setLocation(0);
        company.setCreatedBy(userId);
        this.dao().insert(company);

        Sys_group systemGroup = new Sys_group();
        systemGroup.setTenantId(tenant.getId());
        systemGroup.setName("系统管理组");
        systemGroup.setUnitId(company.getId());
        systemGroup.setUnitPath(company.getPath());
        systemGroup.setCreatedBy(userId);
        this.dao().insert(systemGroup);

        Sys_role adminRole = new Sys_role();
        adminRole.setTenantId(tenant.getId());
        adminRole.setName("租户管理员");
        adminRole.setCode(buildRoleCode(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE, tenant.getId()));
        adminRole.setNote("租户管理员角色");
        adminRole.setDisabled(tenant.isDisabled());
        adminRole.setUnitId(company.getId());
        adminRole.setGroupId(systemGroup.getId());
        adminRole.setCreatedBy(userId);
        this.dao().insert(adminRole);

        Sys_user adminUser = new Sys_user();
        adminUser.setTenantId(tenant.getId());
        adminUser.setSerialNo("0");
        adminUser.setSex(0);
        adminUser.setLoginname(tenant.getAdminLoginname());
        adminUser.setUsername(tenant.getName() + "管理员");
        adminUser.setPassword(adminPassword);
        adminUser.setDisabled(tenant.isDisabled());
        adminUser.setLoginIp("127.0.0.1");
        adminUser.setLoginAt(0L);
        adminUser.setLoginCount(0);
        adminUser.setNeedChangePwd(false);
        adminUser.setDisabledLogin(false);
        adminUser.setUnitId(company.getId());
        adminUser.setUnitPath(company.getPath());
        adminUser.setCreatedBy(userId);
        sysUserService.create(adminUser, new String[]{adminRole.getId()});

        this.dao().update("sys_role_user",
                Chain.make("tenantId", tenant.getId()),
                Cnd.where("userId", "=", adminUser.getId()).and("roleId", "=", adminRole.getId()));
        this.dao().insert("sys_unit_user", Chain.make("id", R.UU32()).add("tenantId", tenant.getId()).add("userId", adminUser.getId()).add("unitId", company.getId()).add("createdBy", userId));

        applyPackageMenus(tenant.getId(), tenantPackage);
    }

    private void applyPackageMenus(String tenantId, Sys_tenant_package tenantPackage) {
        sysTenantMenuSyncService.syncTenantMenus(tenantId, tenantPackage.getId());
    }

    private Sys_unit getTenantCompany(String tenantId) {
        Sys_unit unit = this.dao().fetch(Sys_unit.class, Cnd.where("tenantId", "=", tenantId).and("type", "=", SysUnitType.COMPANY));
        if (unit == null) {
            throw new BaseException("租户单位不存在");
        }
        return unit;
    }

    private Sys_user getTenantAdminUser(Sys_tenant tenant) {
        Sys_user user = this.dao().fetch(Sys_user.class, Cnd.where("tenantId", "=", tenant.getId()).and("loginname", "=", tenant.getAdminLoginname()));
        if (user == null) {
            throw new BaseException("租户管理员不存在");
        }
        return user;
    }

    private Sys_role getRoleByCode(String tenantId, String code) {
        Sys_role role = this.dao().fetch(Sys_role.class, Cnd.where("tenantId", "=", tenantId).and("code", "=", code));
        if (role == null) {
            throw new BaseException("租户默认角色不存在");
        }
        return role;
    }

    private Sys_tenant_package getEnabledPackage(String packageId) {
        Sys_tenant_package tenantPackage = sysTenantPackageService.fetch(packageId);
        if (tenantPackage == null) {
            throw new BaseException("租户套餐不存在");
        }
        if (tenantPackage.isDisabled()) {
            throw new BaseException("租户套餐已被禁用");
        }
        return tenantPackage;
    }

    private void checkLoginnameUnique(String loginname, String excludeUserId) {
        Cnd cnd = Cnd.where("loginname", "=", Strings.sNull(loginname).trim());
        if (Strings.isNotBlank(excludeUserId)) {
            cnd.and("id", "<>", excludeUserId);
        }
        if (this.dao().count(Sys_user.class, cnd) > 0) {
            throw new BaseException("管理员账号已存在");
        }
    }

    private void ensureEditable(String tenantId) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(tenantId)) {
            throw new BaseException("默认平台租户不允许操作");
        }
    }

    private String buildRoleCode(String baseCode, String tenantId) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(tenantId)) {
            return baseCode;
        }
        return baseCode + ":" + tenantId;
    }
}
