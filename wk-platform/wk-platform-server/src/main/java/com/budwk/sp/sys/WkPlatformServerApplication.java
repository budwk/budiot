package com.budwk.sp.sys;

import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.sys.entity.*;
import com.budwk.sp.sys.enums.SysConfigType;
import com.budwk.sp.sys.enums.SysUnitType;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.impl.FileSqlManager;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.random.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        // 排除 Druid 原生的自动配置
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class
})
@EnableAsync
@EnableDiscoveryClient
@EnableCaching
@Slf4j
public class WkPlatformServerApplication implements ApplicationRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private Dao dao;

    @Autowired
    private Environment env;


    public static void main(String[] args) {
        SpringApplication.run(WkPlatformServerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        databaseInit();
    }

    public void databaseInit() {
        try {
            if (0 == dao.count(Sys_user.class)) {
                log.info("开始初始化数据库...");
                Sys_tenant tenant = new Sys_tenant();
                tenant.setId("platform");
                tenant.setName("平台默认");
                tenant.setAdminLoginname("superadmin");
                tenant.setHasExpire(false);
                tenant.setDisabled(false);
                dao.insert(tenant);
                //初始化配置表
                Sys_config conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppName");
                conf.setConfigValue("BudIot开发框架");
                conf.setNote("系统名称");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppShrotName");
                conf.setConfigValue("BudIot");
                conf.setNote("系统短名称");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppVersion");
                conf.setConfigValue("V1.0.0");
                conf.setNote("系统版本号");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppDefault");
                conf.setConfigValue("PLATFORM");
                conf.setNote("前端默认登录APP");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppDomain");
                conf.setConfigValue("http://127.0.0.1:8800");
                conf.setNote("系统域名");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppFileDomain");
                conf.setConfigValue("http://127.0.0.1:9900");
                conf.setNote("文件访问域名");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppFileDomainIn");
                conf.setConfigValue("http://127.0.0.1:9900");
                conf.setNote("文件访问内网域名");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.TEXT);
                conf.setConfigKey("AppUploadBase");
                conf.setConfigValue("/upload");
                conf.setNote("文件访问路径");
                conf.setOpened(true);
                dao.insert(conf);
                conf = new Sys_config();
                conf.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                conf.setType(SysConfigType.BOOL);
                conf.setConfigKey("AppDemoEnv");
                conf.setConfigValue("false");
                conf.setNote("是否演示环境");
                conf.setOpened(true);
                dao.insert(conf);

                Sys_app sysApp = new Sys_app();
                sysApp.setName("系统公用");
                sysApp.setId(GlobalConstant.DEFAULT_COMMON_APPID);
                sysApp.setHidden(true);
                sysApp.setDisabled(false);
                sysApp.setLocation(1);
                dao.insert(sysApp);
                sysApp = new Sys_app();
                sysApp.setName("控制中心");
                sysApp.setId(GlobalConstant.DEFAULT_PLATFORM_APPID);
                sysApp.setHidden(false);
                sysApp.setDisabled(false);
                sysApp.setLocation(2);
                sysApp.setPath("/platform/dashboard");
                dao.insert(sysApp);
                sysApp = new Sys_app();
                sysApp.setName("内容管理");
                sysApp.setId("CMS");
                sysApp.setHidden(false);
                sysApp.setDisabled(false);
                sysApp.setLocation(3);
                sysApp.setPath("/platform/dashboard");
                dao.insert(sysApp);

                //初始化单位
                Sys_unit headUnit = new Sys_unit();
                headUnit.setTenantId(tenant.getId());
                headUnit.setPath("0001");
                headUnit.setName("萌发开源");
                headUnit.setAliasName("BudIot");
                headUnit.setLocation(0);
                headUnit.setAddress("银河-太阳系-地球");
                headUnit.setEmail("");
                headUnit.setTelephone("");
                headUnit.setDisabled(false);
                headUnit.setHasChildren(true);
                headUnit.setParentId("");
                headUnit.setWebsite("https://budwk.com");
                headUnit.setType(SysUnitType.GROUP);
                dao.insert(headUnit);
                Sys_unit unit = new Sys_unit();
                unit.setTenantId(tenant.getId());
                unit.setPath("00010001");
                unit.setName("信息部");
                unit.setAliasName("IT");
                unit.setLocation(1);
                unit.setAddress("银河-太阳系-地球");
                unit.setEmail("wizzer@qq.com");
                unit.setTelephone("");
                unit.setDisabled(false);
                unit.setHasChildren(false);
                unit.setParentId(headUnit.getId());
                unit.setWebsite("https://budwk.com");
                unit.setType(SysUnitType.UNIT);
                dao.insert(unit);

                //初始化角色分组
                Sys_group group = new Sys_group();
                group.setId("SYSTEM");
                group.setTenantId(tenant.getId());
                group.setName("系统管理组");
                group.setUnitId(headUnit.getId());
                group.setUnitPath(headUnit.getPath());
                dao.insert(group);
                group = new Sys_group();
                group.setId("PUBLIC");
                group.setTenantId(tenant.getId());
                group.setName("系统公用组");
                group.setUnitId("");
                group.setUnitPath("");
                dao.insert(group);

                //初始化角色
                Sys_role publicRole = new Sys_role();
                publicRole.setTenantId(tenant.getId());
                publicRole.setName("公共角色");
                publicRole.setCode("public");
                publicRole.setNote("所有用户默认分配");
                publicRole.setDisabled(false);
                publicRole.setUnitId(headUnit.getId());
                publicRole.setGroupId("PUBLIC");
                dao.insert(publicRole);
                Sys_role role = new Sys_role();
                role.setTenantId(tenant.getId());
                role.setName("超级管理员");
                role.setCode(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE);
                role.setNote("超级管理员角色");
                role.setDisabled(false);
                role.setUnitId(headUnit.getId());
                role.setGroupId("SYSTEM");
                dao.insert(role);

                //初始化用户
                Sys_user user = new Sys_user();
                user.setId("5f8cebd7022c409a94e90da1d840b8bb");
                user.setTenantId(tenant.getId());
                user.setSerialNo("0");
                user.setSex(1);
                user.setLoginname(GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME);
                user.setUsername("超级管理员");
                user.setSalt("r5tdr01s7uglfokpsdmtu15602");
                user.setPassword("1bba9287ebc50b766bff84273d11ccefaa7a8da95d078960f05f116e9d970fb0");
                user.setLoginIp("127.0.0.1");
                user.setLoginAt(0L);
                user.setLoginCount(0);
                user.setNeedChangePwd(false);
                user.setNeedChangePwd(false);
                user.setDisabledLogin(false);
                user.setPwdResetAt(System.currentTimeMillis());
                user.setEmail("wizzer@qq.com");
                user.setUnitId(unit.getId());
                user.setUnitPath(unit.getPath());
                user.setCompanyId(headUnit.getId());
                dao.insert(user);

                //初始化用户安全配置
                Sys_user_security security = new Sys_user_security();
                security.setId("MAIN");
                security.setHasEnabled(false);
                security.setPwdLengthMin(6);
                security.setPwdLengthMax(20);
                security.setPwdCharMust(0);
                security.setPwdCharNot("");
                security.setPwdRepeatCheck(false);
                security.setPwdRepeatNum(0);
                security.setPwdRetryLock(false);
                security.setPwdRetryNum(0);
                security.setPwdRetryAction(0);
                security.setPwdRetryTime(0);
                security.setPwdTimeoutDay(0);
                security.setPwdResetChange(false);
                security.setNameRetryLock(false);
                security.setNameRetryNum(3);
                security.setNameTimeout(60);
                security.setUserSessionOnlyOne(false);
                security.setCaptchaHasEnabled(true);
                security.setCaptchaType(0);
                dao.insert(security);

                //不同的插入数据方式(安全)
                dao.insert("sys_role_app", Chain.make("id", R.UU32()).add("appId", GlobalConstant.DEFAULT_COMMON_APPID).add("roleId", role.getId()).add("tenantId",tenant.getId()));
                dao.insert("sys_role_app", Chain.make("id", R.UU32()).add("appId", GlobalConstant.DEFAULT_PLATFORM_APPID).add("roleId", role.getId()).add("tenantId",tenant.getId()));
                dao.insert("sys_role_app", Chain.make("id", R.UU32()).add("appId", "CMS").add("roleId", role.getId()).add("tenantId",tenant.getId()));
                dao.insert("sys_unit_user", Chain.make("id", R.UU32()).add("userId", user.getId()).add("unitId", unit.getId()).add("tenantId",tenant.getId()));
                dao.insert("sys_role_user", Chain.make("id", R.UU32()).add("userId", user.getId()).add("roleId", role.getId()).add("tenantId",tenant.getId()));
                //执行SQL脚本
                FileSqlManager fm = new FileSqlManager("db/");
                fm.setByRow(true);
                List<Sql> sqlList = fm.createCombo(fm.keys());
                Sql[] sqls = sqlList.toArray(new Sql[sqlList.size()]);
                for (Sql sql : sqls) {
                    dao.execute(sql);
                }
                //菜单关联到角色
                List<Sys_menu> list = dao.query(Sys_menu.class, Cnd.where("appId", "=", GlobalConstant.DEFAULT_PLATFORM_APPID));
                for (Sys_menu menu : list) {
                    Sys_role_menu sysRoleMenu = new Sys_role_menu();
                    sysRoleMenu.setRoleId(role.getId());
                    sysRoleMenu.setTenantId(tenant.getId());
                    sysRoleMenu.setAppId(GlobalConstant.DEFAULT_PLATFORM_APPID);
                    sysRoleMenu.setMenuId(menu.getId());
                    dao.insert(sysRoleMenu);
                }
                //菜单关联到角色
                List<Sys_menu> cmsMenuList = dao.query(Sys_menu.class, Cnd.where("appId", "=", "CMS"));
                for (Sys_menu menu : cmsMenuList) {
                    Sys_role_menu sysRoleMenu = new Sys_role_menu();
                    sysRoleMenu.setTenantId(tenant.getId());
                    sysRoleMenu.setRoleId(role.getId());
                    sysRoleMenu.setAppId("CMS");
                    sysRoleMenu.setMenuId(menu.getId());
                    dao.insert(sysRoleMenu);
                }
                List<Sys_menu> cpmMenulist = dao.query(Sys_menu.class, Cnd.where("appId", "=", GlobalConstant.DEFAULT_COMMON_APPID));
                for (Sys_menu menu : cpmMenulist) {
                    //超级管理员角色
                    Sys_role_menu sysRoleMenu = new Sys_role_menu();
                    sysRoleMenu.setTenantId(tenant.getId());
                    sysRoleMenu.setRoleId(role.getId());
                    sysRoleMenu.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                    sysRoleMenu.setMenuId(menu.getId());
                    dao.insert(sysRoleMenu);
                    //公共角色
                    sysRoleMenu = new Sys_role_menu();
                    sysRoleMenu.setTenantId(tenant.getId());
                    sysRoleMenu.setRoleId(publicRole.getId());
                    sysRoleMenu.setAppId(GlobalConstant.DEFAULT_COMMON_APPID);
                    sysRoleMenu.setMenuId(menu.getId());
                    dao.insert(sysRoleMenu);
                }
                log.info("初始化数据库结束...");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
