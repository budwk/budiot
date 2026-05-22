package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.utils.PwdUtil;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantContext;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.dto.SysUserDTO;
import com.budwk.sp.sys.entity.*;
import com.budwk.sp.sys.services.*;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.nutz.lang.Times;
import org.nutz.lang.random.R;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author wizzer@qq.com
 */
@Service
@CacheConfig(cacheNames = "sys_user")
@Slf4j
public class SysUserServiceImpl extends BaseServiceImpl<Sys_user> implements SysUserService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysUserServiceImpl(Dao dao) {
        super(dao);
    }

    @Autowired
    @Lazy
    private SysRoleService sysRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private SysAppService sysAppService;

    @Autowired
    private SysUnitService sysUnitService;

    @Autowired
    private SysUserSecurityService sysUserSecurityService;

    @Autowired
    private SysUserPwdService sysUserPwdService;

    @Autowired
    private WkCacheService wkCacheService;

    @Override
    //@Cacheable(key = "#userId+'_getPermissionList'")
    public List<String> getPermissionList(String userId) {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        this.fetchLinks(user, "roles");
        if (user.getRoles() == null) {
            return new ArrayList<>();
        }
        List<String> permissionList = new ArrayList<String>();
        for (Sys_role role : user.getRoles()) {
            if (!role.isDisabled()) {
                permissionList.addAll(sysRoleService.getPermissionList(role));
            }
        }
        String publicRoleId = sysRoleService.getPublicId();
        // 全体用户追加公共角色（用户中心、站内信）
        if (Strings.isNotBlank(publicRoleId)) {
            WkDaoTenantContext.withoutTenant(()->{
                Sql sql = Sqls.create("select distinct a.permission from sys_menu a,sys_role_menu b where a.id=b.menuId and " +
                        " b.roleId=@pubRoleId and a.disabled=@f ");
                sql.params().set("pubRoleId", publicRoleId);
                sql.params().set("f", false);
                sql.setCallback(Sqls.callback.strs());
                this.dao().execute(sql);
                permissionList.addAll(sql.getList(String.class));
            });
        }
        return permissionList;
    }

    @Override
    @Cacheable(key = "#userId+'_getRoleList'")
    public List<String> getRoleList(String userId) {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        this.fetchLinks(user, "roles");
        if (user.getRoles() == null) {
            return new ArrayList<>();
        }
        Set<String> roleList = new LinkedHashSet<>();
        for (Sys_role role : user.getRoles()) {
            if (!role.isDisabled()) {
                roleList.add(role.getCode());
            }
        }
        // 追加public公共角色权限
        roleList.add("public");
        return new ArrayList<>(roleList);
    }

    @Override
    @Cacheable(key = "#userId+'_getMenuList'")
    public List<Sys_menu> getMenuList(String userId) {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        Sql sql = Sqls.create("select distinct a.* from sys_menu a,sys_role_menu b where a.id=b.menuId and " +
                " (b.roleId=@pubRoleId or b.roleId in(select c.roleId from sys_user_role c,sys_role d where c.roleId=d.id and c.userId=@userId and d.disabled=@f) ) and a.disabled=@f order by a.location ASC,a.path asc");
        sql.params().set("pubRoleId", sysRoleService.getPublicId());
        sql.params().set("userId", userId);
        sql.params().set("f", false);
        return sysMenuService.listEntity(sql);
    }

    @Override
    @Cacheable(key = "#userId+'_'+#appId+'_getMenuList'")
    public List<Sys_menu> getMenuList(String userId, String appId) {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        Sql sql = Sqls.create("select distinct a.* from sys_menu a,sys_role_menu b where a.appId=@appId and a.id=b.menuId and " +
                " (b.roleId=@pubRoleId or b.roleId in(select c.roleId from sys_role_user c,sys_role d where c.roleId=d.id and c.userId=@userId and d.disabled=@f) ) and a.disabled=@f order by a.location ASC,a.path asc");
        sql.params().set("pubRoleId", sysRoleService.getPublicId());
        sql.params().set("userId", userId);
        sql.params().set("appId", appId);
        sql.params().set("f", false);
        return sysMenuService.listEntity(sql);
    }

    @Override
    @Cacheable(key = "#userId+'_getAppList'")
    public List<Sys_app> getAppList(String userId) {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        Sql sql = Sqls.create("select distinct a.id,a.name,a.path,a.hidden,a.disabled,a.location from sys_app a,sys_role_app b where a.id=b.appId and " +
                " (b.roleId=@pubRoleId or b.roleId in(select c.roleId from sys_role_user c,sys_role d where c.roleId=d.id and c.userId=@userId and d.disabled=@f) ) and a.disabled=@f order by a.location");
        sql.params().set("pubRoleId", sysRoleService.getPublicId());
        sql.params().set("userId", userId);
        sql.params().set("f", false);
        return sysAppService.listEntity(sql);
    }

    @Override
    public List<Sys_menu> getMenusAndDatas(String userId, String appId) {
        Sql sql = Sqls.create("select distinct a.* from sys_menu a,sys_role_menu b where a.id=b.menuId and a.appId=@appId and " +
                "(b.roleId=@pubRoleId or b.roleId in(select c.roleId from sys_role_user c,sys_role d where c.roleId=d.id and c.userId=@userId and d.disabled=@f) ) and a.disabled=@f order by a.location ASC,a.path asc");
        sql.params().set("pubRoleId", sysRoleService.getPublicId());
        sql.params().set("userId", userId);
        sql.params().set("appId", appId);
        sql.params().set("f", false);
        return sysMenuService.listEntity(sql);
    }

    @Override
    public void checkLoginname(String loginname) throws BaseException {
        checkLoginname(loginname, null);
    }

    @Override
    public void checkLoginname(String loginname, String tenantId) throws BaseException {
        Cnd cnd = Cnd.where("loginname", "=", loginname);
        WkDaoTenantContext.withoutTenant(()->{
            if (this.count(cnd) < 1) {
                throw new BaseException("用户名不存在");
            }
        });
    }

    @Override
    public void checkMobile(String mobile) throws BaseException {
        checkMobile(mobile, null);
    }

    @Override
    public void checkMobile(String mobile, String tenantId) throws BaseException {
        Cnd cnd = Cnd.where("mobile", "=", mobile);
        WkDaoTenantContext.withoutTenant(()->{
            if (this.count(cnd) < 1) {
                throw new BaseException("手机号不存在");
            }
        });
    }

    @Override
    public void checkPwdTimeout(String userId, Long pwdResetAt) throws BaseException {
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && security.getHasEnabled() && security.getPwdTimeoutDay() > 0 && pwdResetAt != null) {
            if (System.currentTimeMillis() > pwdResetAt + (security.getPwdTimeoutDay() * 86400 * 1000)) {
                throw new BaseException("密码已过期，请及时修改");
            }
        }
    }

    @Override
    public void checkPassword(Sys_user user, String pwd) throws BaseException {
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && security.getHasEnabled()) {
            if (Strings.sNull(pwd).length() < security.getPwdLengthMin()) {
                throw new BaseException("密码最小长度为" + security.getPwdLengthMin());
            }
            if (Strings.sNull(pwd).length() > security.getPwdLengthMax()) {
                throw new BaseException("密码最大长度为" + security.getPwdLengthMax());
            }
            // 密码必须同时包含字母和数字，可以含特殊字符
            if (1 == security.getPwdCharMust()) {
                String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)(?!\\W+$)(?![0-9\\W]+$)(?![a-zA-Z\\W]+$)[0-9A-Za-z\\W]*$";
                if (!pwd.matches(regex)) {
                    throw new BaseException("密码必须同时包含字母和数字");
                }
            }
            // 必须同时包含大写字母、小写字母、和数字，可以含特殊字符
            if (2 == security.getPwdCharMust()) {
                String regex = "^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!\\W+$)(?![a-zA-Z]+$)(?![0-9a-z\\W]+$)(?![0-9A-Z\\W]+$)(?![a-zA-Z\\W]+$)[0-9A-Za-z\\W]*$";
                if (!pwd.matches(regex)) {
                    throw new BaseException("密码必须同时包含大写字母、小写字母、和数字");
                }
            }
            // 必须同时包含大写、小写字母、特殊字符、数字
            if (3 == security.getPwdCharMust()) {
                String regex = "^(?![A-Za-z0-9]+$)(?![a-z0-9\\W]+$)(?![A-Za-z\\W]+$)(?![A-Z0-9\\W]+$)[a-zA-Z0-9\\W]*$";
                if (!pwd.matches(regex)) {
                    throw new BaseException("密码必须同时包含大写、小写字母、特殊字符、数字");
                }
            }
            if (Strings.isNotBlank(security.getPwdCharNot())) {
                if (security.getPwdCharNot().contains("loginname") && Strings.isNotBlank(user.getLoginname()) && pwd.contains(user.getLoginname())) {
                    throw new BaseException("密码中不可包含用户名");
                }
                if (security.getPwdCharNot().contains("email") && Strings.isNotBlank(user.getEmail()) && pwd.toLowerCase().contains(user.getEmail())) {
                    throw new BaseException("密码中不可包含用户电子邮箱");
                }
                if (security.getPwdCharNot().contains("mobile") && Strings.isNotBlank(user.getEmail()) && pwd.toLowerCase().contains(user.getEmail())) {
                    throw new BaseException("密码中不可包含用户手机号码");
                }
            }
            if (security.getPwdRepeatCheck()) {
                List<Sys_user_pwd> pwdList = sysUserPwdService.query(Cnd.where("userId", "=", user.getId()));
                for (Sys_user_pwd userPwd : pwdList) {
                    if (userPwd.getPassword().equals(PwdUtil.getPassword(pwd, userPwd.getSalt()))) {
                        throw new BaseException("您在系统中曾经使用过此密码，请更换");
                    }
                }
            }
        }
    }

    @Override
    public Sys_user loginByPassword(String loginname, String passowrd) throws BaseException {
        return loginByPassword(loginname, passowrd, null);
    }

    @Override
    public Sys_user loginByPassword(String loginname, String passowrd, String tenantId) throws BaseException {
        Cnd cnd = Cnd.where("loginname", "=", loginname);
        if (Strings.isNotBlank(tenantId)) {
            cnd.and("tenantId", "=", tenantId);
        }
        Sys_user user = this.fetch(cnd);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        if (user.isDisabled()) {
            throw new BaseException("用户被禁用");
        }
        if (user.isDisabledLogin() && user.getDisabledLoginAt() != null) {
            Sys_user_security security = sysUserSecurityService.getWithCache();
            // 是否临时被禁用,只判断 pwdRetryTime 值,不判断锁定账号是否启用==启用后对上次设置有效
            if (security != null && security.getHasEnabled() && security.getPwdRetryTime() > 0) {
                if (System.currentTimeMillis() > user.getDisabledLoginAt() + (security.getPwdRetryTime() * 1000)) {
                    wkCacheService.deleteCache(GlobalConstant.CACHE_PREFIX + user.getId() + ":pwdretrynum");
                    this.update(Chain.make("disabledLogin", false).add("disabledLoginAt", null), Cnd.where("id", "=", user.getId()));
                } else {
                    throw new BaseException("禁止登录，解锁时间：" + Times.format("MM月dd HH:mm:ss", new Date(user.getDisabledLoginAt() + (security.getPwdRetryTime() * 1000))));
                }
            }
        }
        String hashedPassword = PwdUtil.getPassword(passowrd, user.getSalt());
        if (!Strings.sNull(hashedPassword).equalsIgnoreCase(user.getPassword())) {
            // 一天内密码错误次数超过最大重试次数锁定账号
            Sys_user_security security = sysUserSecurityService.getWithCache();
            if (security != null && security.getHasEnabled() && security.getPwdRetryLock() && security.getPwdRetryNum() > 0) {
                int errNum = Integer.parseInt(Strings.sNull(wkCacheService.getCache(GlobalConstant.CACHE_PREFIX + user.getId() + ":pwdretrynum"), "0"));
                wkCacheService.setCache(GlobalConstant.CACHE_PREFIX + user.getId() + ":pwdretrynum", "" + (errNum + 1), 24, TimeUnit.HOURS);
                // 重试次数大于配置
                if (errNum + 1 > security.getPwdRetryNum()) {
                    // 锁定账户
                    if (1 == security.getPwdRetryAction()) {
                        this.update(Chain.make("disabled", true).add("updatedAt", System.currentTimeMillis()), Cnd.where("id", "=", user.getId()));
                    }
                    // 指定时间禁止登录
                    if (2 == security.getPwdRetryAction() && security.getPwdRetryTime() > 0) {
                        this.update(Chain.make("disabledLogin", true).add("disabledLoginAt", System.currentTimeMillis()), Cnd.where("id", "=", user.getId()));
                    }
                }
            }
            throw new BaseException("密码不正确");
        }
        wkCacheService.deleteCache(GlobalConstant.CACHE_PREFIX + user.getId() + ":pwdretrynum");
        return user;
    }

    @Override
    public Sys_user loginByMobile(String mobile) throws BaseException {
        return loginByMobile(mobile, null);
    }

    @Override
    public Sys_user loginByMobile(String mobile, String tenantId) throws BaseException {
        Cnd cnd = Cnd.where("mobile", "=", mobile);
        if (Strings.isNotBlank(tenantId)) {
            cnd.and("tenantId", "=", tenantId);
        }
        Sys_user user = this.fetch(cnd);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        if (user.isDisabled()) {
            throw new BaseException("用户被禁用");
        }
        return user;
    }

    // 这里不能用缓存,因为没有 userId 没法用前缀清除,会造成缓存清除不干净
    public Sys_user getUserByLoginname(String loginname) throws BaseException {
        return getUserByLoginname(loginname, null);
    }

    @Override
    public Sys_user getUserByLoginname(String loginname, String tenantId) throws BaseException {
        Cnd cnd = Cnd.where("loginname", "=", loginname);
        if (Strings.isNotBlank(tenantId)) {
            cnd.and("tenantId", "=", tenantId);
        }
        Sys_user user = this.fetch(cnd);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        return user;
    }

    @Cacheable(key = "#userId+'_getUserById'")
    public Sys_user getUserById(String userId) throws BaseException {
        Sys_user user = this.fetch(userId);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        return user;
    }

    public void setPwdByLoginname(String loginname, String password) throws BaseException {
        setPwdByLoginname(loginname, null, password);
    }

    @Override
    public void setPwdByLoginname(String loginname, String tenantId, String password) throws BaseException {
        Sys_user user = getUserByLoginname(loginname, tenantId);
        this.checkPassword(user, password);
        String salt = R.UU32();
        String dbpwd = PwdUtil.getPassword(password, salt);
        Cnd cnd = Cnd.where("loginname", "=", loginname);
        if (Strings.isNotBlank(tenantId)) {
            cnd.and("tenantId", "=", tenantId);
        }
        this.update(Chain.make("salt", salt).add("password", dbpwd).add("pwdResetAt", System.currentTimeMillis()), cnd);
        this.cacheClear();
        this.recordPwd(user.getId(), dbpwd, salt);
    }

    public void setPwdById(String id, String password) throws BaseException {
        Sys_user user = this.fetch(id);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        this.checkPassword(user, password);
        String salt = R.UU32();
        String dbpwd = PwdUtil.getPassword(password, salt);
        this.update(Chain.make("salt", salt).add("password", dbpwd).add("pwdResetAt", System.currentTimeMillis()),
                Cnd.where("id", "=", id));
        this.cacheClear();
        this.recordPwd(user.getId(), dbpwd, salt);
    }

    @CacheEvict(key = "#id + '_getUserById'")
    public void setThemeConfig(String id, String themeConfig) {
        this.update(Chain.make("themeConfig", themeConfig), Cnd.where("id", "=", id));
        log.debug("更新用户 {} 的主题配置并清理相关缓存", id);
    }

    public void setLoginInfo(String userId, String ip) {
        this.update(Chain.make("loginIp", ip).add("loginAt", System.currentTimeMillis()).addSpecial("loginCount", "+1"), Cnd.where("id", "=", userId));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void create(Sys_user user, String[] roleIds) {
        String password = user.getPassword();
        this.checkPassword(user, password);
        String salt = R.UU32();
        String dbpwd = PwdUtil.getPassword(password, salt);
        user.setPassword(dbpwd);
        user.setSalt(salt);
        user.setSex(user.getSex() == null ? 0 : user.getSex());
        user.setLoginCount(0);
        user.setNeedChangePwd(false);
        // 后台重置密码后下次登录是否强制修改密码
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && security.getHasEnabled() && security.getPwdRepeatCheck() && security.getPwdRepeatNum() > 0) {
            user.setNeedChangePwd(true);
        }
        user.setPwdResetAt(System.currentTimeMillis());
        user.setCompanyId(sysUnitService.getMasterCompanyId(user.getUnitId()));
        this.insert(user);
        if (roleIds != null) {
            for (String roleId : roleIds) {
                this.dao().insert("sys_role_user", Chain.make("id", R.UU32()).add("tenantId", user.getTenantId()).add("userId", user.getId()).add("roleId", roleId));
            }
        }
        this.recordPwd(user.getId(), dbpwd, salt);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void update(Sys_user user, String[] roleIds) {
        user.setPassword(null);
        user.setSalt(null);
        this.updateIgnoreNull(user);
        this.dao().clear("sys_role_user", Cnd.where("userId", "=", user.getId()));
        if (roleIds != null) {
            for (String roleId : roleIds) {
                this.dao().insert("sys_role_user", Chain.make("id", R.UU32()).add("tenantId", user.getTenantId()).add("userId", user.getId()).add("roleId", roleId));
            }
        }
        this.cacheClear();
    }

    @Override
    public String resetPwd(String userId) {
        boolean needChangePwd = false;
        String salt = R.UU32();
        String password = PwdUtil.generateRandomString(12);
        // 后台重置密码后下次登录是否强制修改密码
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && security.getHasEnabled() && security.getPwdRepeatCheck() && security.getPwdRepeatNum() > 0) {
            needChangePwd = true;
        }
        String dbpwd = PwdUtil.getPassword(password, salt);
        this.update(Chain.make("password", dbpwd)
                .add("salt", salt).add("needChangePwd", needChangePwd), Cnd.where("id", "=", userId));
        this.cacheClear();
        this.recordPwd(userId, dbpwd, salt);
        return password;
    }

    @Override
    public String resetPwd(String userId, String password, boolean needChangePwd) {
        this.checkPassword(this.fetch(userId), password);
        String salt = R.UU32();
        String dbpwd = PwdUtil.getPassword(password, salt);
        this.update(Chain.make("password", dbpwd)
                .add("salt", salt).add("needChangePwd", needChangePwd), Cnd.where("id", "=", userId));
        this.cacheClear();
        this.recordPwd(userId, dbpwd, salt);
        return password;
    }

    @Async
    public void recordPwd(String userId, String password, String salt) {
        Sys_user_security security = sysUserSecurityService.getWithCache();
        if (security != null && security.getHasEnabled() && security.getPwdRepeatCheck() && security.getPwdRepeatNum() > 0) {
            Sys_user_pwd userPwd = new Sys_user_pwd();
            userPwd.setUserId(userId);
            userPwd.setPassword(password);
            userPwd.setSalt(salt);
            sysUserPwdService.insert(userPwd);
            Sql sql = Sqls.create("delete from sys_user_pwd where userId=@userId and id not in(select t.id FROM (SELECT * from sys_user_pwd where userId=@userId order by createdAt desc limit $num)as t)");
            sql.setParam("userId", userId);
            sql.setVar("num", security.getPwdRepeatNum());
            sysUserPwdService.execute(sql);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void deleteUser(String userId) {
        this.dao().clear("sys_role_user", Cnd.where("userId", "=", userId));
        this.dao().clear("sys_unit_user", Cnd.where("userId", "=", userId));
        this.delete(userId);
        this.cacheClear();
    }


    @Override
    public String importUser(List<SysUserDTO> userList, String password, Boolean isUpdateSupport, String userId) {
        if (userList == null || userList.size() == 0) {
            throw new BaseException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUserDTO dto : userList) {
            try {
                Sys_user user = new Sys_user();
                BeanUtils.copyProperties(dto, user);
                if (Strings.isNotBlank(dto.getImportUnitName())) {
                    Sys_unit dbUnit = sysUnitService.fetch(Cnd.where("name", "=", Strings.trim(dto.getImportUnitName())));
                    if (dbUnit != null) {
                        user.setUnitId(dbUnit.getId());
                        user.setUnitPath(dbUnit.getPath());
                    }
                }
                Sys_user dbUser = fetch(Cnd.where("loginname", "=", user.getLoginname()));
                if (dbUser == null) {
                    if (count(Cnd.where("mobile", "=", user.getMobile())) > 0) {
                        failureNum++;
                        failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " 手机号已存在");
                        continue;
                    }
                    if (count(Cnd.where("email", "=", user.getEmail())) > 0) {
                        failureNum++;
                        failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " Email已存在");
                        continue;
                    }
                    String salt = R.UU32();
                    String pwd = password;
                    if (Strings.isBlank(pwd)) {
                        pwd = PwdUtil.generateRandomString(12);
                    }
                    user.setPassword(PwdUtil.getPassword(pwd, salt));
                    user.setSalt(salt);
                    user.setCreatedBy(userId);
                    this.insert(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、用户 " + user.getLoginname() + "（密码：" + pwd + "） 导入成功");
                } else if (isUpdateSupport) {
                    if (!GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(user.getLoginname())) {
                        if (count(Cnd.where("mobile", "=", user.getMobile()).and("loginname", "<>", user.getLoginname())) > 0) {
                            failureNum++;
                            failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " 手机号已存在");
                            continue;
                        }
                        if (count(Cnd.where("email", "=", user.getEmail()).and("loginname", "<>", user.getLoginname())) > 0) {
                            failureNum++;
                            failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " Email已存在");
                            continue;
                        }
                        user.setId(dbUser.getId());
                        user.setUpdatedBy(userId);
                        this.updateIgnoreNull(user);
                        successNum++;
                        successMsg.append("<br/>" + successNum + "、账号 " + user.getLoginname() + " 更新成功");
                    } else {
                        failureNum++;
                        failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " 系统管理员不允许操作");
                    }
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getLoginname() + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + Strings.sNull(dto.getLoginname()) + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉， 导入成功 " + successNum + " 条，导入失败 " + failureNum + " 条，错误如下：");
            if (successNum > 0) {
                throw new BaseException(failureMsg.toString() + "<br/><br/>成功列表：" + successMsg.toString());
            } else {
                throw new BaseException(failureMsg.toString());
            }
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    @CacheEvict(allEntries = true)
    @Async
    public void cacheClear() {

    }
}
