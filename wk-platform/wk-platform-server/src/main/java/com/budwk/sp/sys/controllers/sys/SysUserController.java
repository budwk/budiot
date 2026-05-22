package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantContext;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.excel.utils.ExcelUtil;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysUserDTO;
import com.budwk.sp.sys.entity.Sys_role;
import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.services.SysGroupService;
import com.budwk.sp.sys.services.SysPostService;
import com.budwk.sp.sys.services.SysUnitService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/user")
@SLog(tag = "用户管理")
@Tag(name = "用户管理", description = "用户管理接口")
public class SysUserController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SysUserController.class);
    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysPostService sysPostService;
    @Autowired
    private SysGroupService sysGroupService;

    @GetMapping("/unitlist")
    @Operation(summary = "Vue3树形列表查询")
    @SaCheckPermission("sys.manage.user")
    public Result<?> getUnitList(
            @Parameter(description = "单位名称") @RequestParam(required = false) String name) {
        Cnd cnd = Cnd.NEW();
        if (!StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            String unitId = StpUtil.getSession().getString("unitId");
            Sys_unit unit = sysUnitService.fetch(unitId);
            cnd.and("path", "like", unit.getPath() + "%");
        }
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        cnd.asc("location");
        cnd.asc("path");
        return Result.success().addData(sysUnitService.query(cnd));
    }

    @GetMapping("/unit")
    @Operation(summary = "Vue2获取单位树数据")
    @SaCheckPermission("sys.manage.user")
    public Result<?> getUnitTree() {
        String pid = "";
        List<Sys_unit> list;
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            Cnd cnd = Cnd.NEW();
            cnd.asc("location").asc("path");
            list = sysUnitService.query(cnd);
        } else {
            String unitId = StpUtil.getSession().getString("unitId");
            Sys_unit unit = sysUnitService.fetch(sysUnitService.getMasterCompanyId(unitId));
            pid = unit.getParentId();
            list = sysUnitService.query(Cnd.where("path", "like", unit.getPath() + "%").asc("location").asc("path"));
        }
        NutMap unitMap = NutMap.NEW();
        for (Sys_unit unit : list) {
            List<Sys_unit> list1 = unitMap.getList(unit.getParentId(), Sys_unit.class);
            if (list1 == null) {
                list1 = new ArrayList<>();
            }
            list1.add(unit);
            unitMap.put(unit.getParentId(), list1);
        }
        return Result.data(buildTree(unitMap, pid));
    }

    private List<NutMap> buildTree(NutMap unitMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_unit> subList = unitMap.getList(pid, Sys_unit.class);
        for (Sys_unit unit : subList) {
            NutMap map = Lang.obj2nutmap(unit);
            map.put("label", unit.getName());
            if (unit.isHasChildren() || (unitMap.get(unit.getId()) != null)) {
                map.put("children", buildTree(unitMap, unit.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @GetMapping("/post")
    @Operation(summary = "获取职务列表")
    @SaCheckPermission("sys.manage.user")
    public Result<?> post() {
        return Result.data(sysPostService.query());
    }

    @GetMapping("/count/{unitPath}")
    @Operation(summary = "获取单位用户统计")
    @SaCheckPermission("sys.manage.user")
    public Result<?> count(@Parameter(description = "单位PATH") @PathVariable String unitPath) {
        int allNumber = sysUserService.count(Cnd.where("unitPath", "like", unitPath + "%"));
        int enabledNumber = sysUserService.count(Cnd.where("unitPath", "like", unitPath + "%").and("disabled", "=", false));
        int disabledNumber = sysUserService.count(Cnd.where("unitPath", "like", unitPath + "%").and("disabled", "=", true));
        return Result.data(NutMap.NEW().addv("allNumber", allNumber).addv("enabledNumber", enabledNumber).addv("disabledNumber", disabledNumber));
    }

    @PostMapping("/list")
    @Operation(summary = "获取用户列表")
    @SaCheckPermission("sys.manage.user")
    public Result<?> list(
            @Parameter(description = "开始时间") @RequestParam(required = false) Long beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Long endTime,
            @Parameter(description = "用户状态") @RequestParam(required = false) Boolean disabled,
            @Parameter(description = "手机号码") @RequestParam(required = false) String mobile,
            @Parameter(description = "单位PATH") @RequestParam(required = false) String unitPath,
            @Parameter(description = "职务ID") @RequestParam(required = false) String postId,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "用户名") @RequestParam(required = false) String loginname,
            @Parameter(description = "查询关键词") @RequestParam(required = false) String query,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = buildUserQueryCnd(beginTime, endTime, disabled, mobile, unitPath, postId, username, loginname, query);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(sysUserService.listPageLinks(pageNo, pageSize, cnd, "^(unit|roles|createdByUser|updatedByUser)$"));
    }

    @GetMapping("/number")
    @Operation(summary = "获取员工编号")
    @SaCheckPermission("sys.manage.user")
    public Result<?> number() {
        Sql sql = Sqls.create("SELECT MAX(CAST(serialno AS BIGINT)) FROM sys_user");
        sql.setCallback(Sqls.callback.integer());
        sysUserService.dao().execute(sql);
        return Result.data(sql.getInt() + 1);
    }

    @GetMapping("/group")
    @Operation(summary = "获取公司(或为上级单位)角色组及角色")
    @SaCheckPermission("sys.manage.user")
    public Result<?> group(
            @Parameter(description = "单位ID") @RequestParam String unitId) {
        if (StpUtil.hasPermission("sys.manage.role.system")) {
            return Result.data(sysGroupService.query(Cnd.where("unitId", "=",
                            sysUnitService.getMasterCompanyId(unitId)).or("unitid", "=", "").asc("createdAt"), "roles",
                    Cnd.where("code", "not like", "public%")));
        } else {
            return Result.data(sysGroupService.query(Cnd.where("unitId", "=",
                            sysUnitService.getMasterCompanyId(unitId)).asc("createdAt"), "roles",
                    Cnd.where("code", "not like", "public%")));
        }
    }

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.create")
    public Result<?> create(@RequestBody @Validated SysUserDTO dto) {
        Sys_user user = new Sys_user();
        BeanUtils.copyProperties(dto, user);
        AtomicInteger checkNumber = new AtomicInteger(sysUserService.count(Cnd.where("serialNo", "=", user.getSerialNo())));
        if (checkNumber.get() > 0) {
            return Result.error("用户编号已存在");
        }
        if (Strings.isNotBlank(user.getMobile())) {
            // 忽略租户条件，手机号全平台唯一
            WkDaoTenantContext.withoutTenant(()-> {
                checkNumber.set(sysUserService.count(Cnd.where("mobile", "=", user.getMobile())));
            });
            if (checkNumber.get() > 0) {
                return Result.error("手机号已存在");
            }
        }
        checkNumber.set(0);
        // 忽略租户条件，用户名全平台唯一
        WkDaoTenantContext.withoutTenant(()->{
            checkNumber.set(sysUserService.count(Cnd.where("loginname", "=", Strings.trim(user.getLoginname()))));
        });
        if (checkNumber.get() > 0) {
            return Result.error("用户名已存在");
        }
        if (Strings.isNotBlank(user.getEmail())) {
            // 忽略租户条件，邮箱全平台唯一
            WkDaoTenantContext.withoutTenant(()->{
                checkNumber.set(sysUserService.count(Cnd.where("email", "=", Strings.trim(user.getEmail()))));
            });
            if (Strings.isNotBlank(Strings.trim(user.getEmail())) && checkNumber.get() > 0) {
                return Result.error("邮箱已存在");
            }
        }
        user.setCreatedBy(StpUtil.getLoginIdAsString());
        sysUserService.create(user, dto.getRoleIds());
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改用户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysUserDTO dto) {
        Sys_user user = new Sys_user();
        BeanUtils.copyProperties(dto, user);
        int checkNumber = sysUserService.count(Cnd.where("serialNo", "=", user.getSerialNo()).and("id", "<>", user.getId()));
        if (checkNumber > 0) {
            return Result.error("用户编号已存在");
        }
        if (Strings.isNotBlank(user.getMobile())) {
            checkNumber = sysUserService.count(Cnd.where("mobile", "=", user.getMobile()).and("id", "<>", user.getId()));
            if (checkNumber > 0) {
                return Result.error("手机号已存在");
            }
        }
        if (Strings.isNotBlank(user.getLoginname())) {
            checkNumber = sysUserService.count(Cnd.where("loginname", "=", Strings.trim(user.getLoginname())).and("id", "<>", user.getId()));
            if (checkNumber > 0) {
                return Result.error("用户名已存在");
            }
        }
        if (Strings.isNotBlank(user.getEmail())) {
            checkNumber = sysUserService.count(Cnd.where("email", "=", Strings.trim(user.getEmail())).and("id", "<>", user.getId()));
            if (Strings.isNotBlank(Strings.trim(user.getEmail())) && checkNumber > 0) {
                return Result.error("邮箱已存在");
            }
        }
        if (user.isDisabled() && GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(Strings.trim(user.getLoginname()))) {
            return Result.error("超级管理员不可禁用");
        }
        user.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysUserService.update(user, dto.getRoleIds());
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取用户信息")
    @SaCheckPermission("sys.manage.user")
    public Result<?> getData(@Parameter(description = "用户ID") @PathVariable String id) {
        Sys_user user = sysUserService.fetch(id);
        if (user == null) {
            return Result.error("数据不存在");
        }
        user = sysUserService.fetchLinks(user, "^(unit|roles)$");
        List<Sys_role> roles = user.getRoles();
        List<String> roleIds = new ArrayList<>();
        for (Sys_role role : roles) {
            roleIds.add(role.getId());
        }
        return Result.data(NutMap.NEW().addv("user", user).addv("roleIds", roleIds));
    }

    @GetMapping("/reset_pwd/{id}")
    @Operation(summary = "重置用户密码")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.update")
    public Result<?> resetPwd(@Parameter(description = "用户ID") @PathVariable String id) {
        return Result.data(sysUserService.resetPwd(id));
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.update")
    public Result<?> changeDisabled(
            @Parameter(description = "主键ID") @RequestParam String id,
            @Parameter(description = "用户名") @RequestParam String loginname,
            @Parameter(description = "disabled=true禁用") @RequestParam boolean disabled) {
        if (GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(loginname)) {
            return Result.error("超级管理员不可禁用");
        }
        int res = sysUserService.update(Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        sysUserService.cacheClear();
        if (res > 0) {
            return Result.success();
        }
        return Result.error();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.delete")
    public Result<?> delete(
            @Parameter(description = "主键ID") @PathVariable String id,
            @Parameter(description = "用户名") @RequestParam String loginname) {
        if (GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(loginname)) {
            return Result.error("超级管理员不可删除");
        }
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/delete_more")
    @Operation(summary = "批量删除用户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.user.delete")
    public Result<?> deleteMore(
            @Parameter(description = "用户ID数组") @RequestParam String[] ids) {
        String superadminId = "";
        Sys_user user = sysUserService.fetch(Cnd.where("loginname", "=", GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME));
        if (user != null) {
            superadminId = user.getId();
        }
        if (ids != null) {
            for (String id : ids) {
                if (superadminId.equals(id)) {
                    return Result.error("超级管理员用户不可删除");
                }
                sysUserService.deleteUser(id);
            }
        }
        return Result.success();
    }

    @PostMapping("/export")
    @Operation(summary = "导出用户数据")
    @SaCheckPermission("sys.manage.user.export")
    public void export(
            @Parameter(description = "用户状态") @RequestParam(required = false) Boolean disabled,
            @Parameter(description = "开始时间") @RequestParam(required = false) String beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "手机号码") @RequestParam(required = false) String mobile,
            @Parameter(description = "用户名") @RequestParam(required = false) String loginname,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "单位PATH") @RequestParam(required = false) String unitPath,
            @Parameter(description = "职务ID") @RequestParam(required = false) String postId,
            @Parameter(description = "查询关键词") @RequestParam(required = false) String query,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy,
            HttpServletResponse response) {
        Cnd cnd = buildUserQueryCnd(parseLongParam(beginTime), parseLongParam(endTime), disabled, mobile, unitPath, postId, username, loginname, query);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        try {
            List<Sys_user> list = sysUserService.query(cnd, "^(unit|post)$");
            ExcelUtil<Sys_user> util = new ExcelUtil<>(Sys_user.class);
            util.exportExcel(response, list, "用户数据");
        } catch (Exception e) {
            log.error("导出用户数据失败", e);
        }
    }


    @PostMapping("/importData")
    @Operation(summary = "导入用户数据")
    @SaCheckPermission("sys.manage.user.import")
    public Result<?> importData(
            @Parameter(description = "导入文件") @RequestParam("Filedata") MultipartFile file,
            @Parameter(description = "默认密码") @RequestParam(required = false) String pwd,
            @Parameter(description = "是否更新已存在用户") @RequestParam(defaultValue = "false") Boolean updateSupport) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.error("导入文件不能为空");
        }
        ExcelUtil<SysUserDTO> util = new ExcelUtil<>(SysUserDTO.class);
        List<SysUserDTO> list = util.importExcel(file.getInputStream());
        return Result.success(sysUserService.importUser(list, pwd, updateSupport, StpUtil.getLoginIdAsString()));
    }

    @PostMapping("/importTemplate")
    @Operation(summary = "下载用户导入模板")
    @SaCheckLogin
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<SysUserDTO> util = new ExcelUtil<>(SysUserDTO.class);
        util.importTemplateExcel(response, "用户数据");
    }

    private Cnd buildUserQueryCnd(Long beginTime, Long endTime, Boolean disabled, String mobile, String unitPath,
                                  String postId, String username, String loginname, String query) {
        Cnd cnd = Cnd.NEW();
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            if (Strings.isNotBlank(unitPath)) {
                cnd.and("unitPath", "like", unitPath + "%");
            }
        } else if (Strings.isNotBlank(unitPath)) {
            cnd.and("unitPath", "like", unitPath + "%");
        } else {
            String myUnitPath = StpUtil.getSession().getString("unitPath");
            cnd.and("unitPath", "like", myUnitPath + "%");
        }
        if (Strings.isNotBlank(postId)) {
            cnd.and("postId", "=", postId);
        }
        if (Strings.isNotBlank(username)) {
            cnd.and("username", "like", "%" + username + "%");
        }
        if (Strings.isNotBlank(loginname)) {
            cnd.and("loginname", "like", "%" + loginname + "%");
        }
        if (Strings.isNotBlank(mobile)) {
            cnd.and("mobile", "like", "%" + mobile + "%");
        }
        if (beginTime != null && endTime != null) {
            cnd.and("createdAt", ">=", beginTime);
            cnd.and("createdAt", "<=", endTime);
        }
        if (disabled != null) {
            cnd.and("disabled", "=", disabled);
        }
        if (Strings.isNotBlank(query)) {
            cnd.and(Cnd.exps("loginname", "like", "%" + query + "%")
                    .or("username", "like", "%" + query + "%")
                    .or("mobile", "like", "%" + query + "%"));
        }
        return cnd;
    }

    private Long parseLongParam(String value) {
        if (!StringUtils.hasText(value) || "NaN".equalsIgnoreCase(value)) {
            return null;
        }
        return Long.parseLong(value);
    }
}
