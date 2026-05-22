package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysGroupDTO;
import com.budwk.sp.sys.dto.SysRoleDTO;
import com.budwk.sp.sys.entity.Sys_group;
import com.budwk.sp.sys.entity.Sys_menu;
import com.budwk.sp.sys.entity.Sys_role;
import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.enums.SysUnitType;
import com.budwk.sp.sys.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/role")
@SLog(tag = "角色管理")
@Tag(name = "角色管理", description = "角色管理接口")
public class SysRoleController {
    @Autowired
    private SysRoleService sysRoleService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysGroupService sysGroupService;
    @Autowired
    private SysAppService sysAppService;
    @Autowired
    private SysPostService sysPostService;
    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/unit")
    @Operation(summary = "获取公司单位数据")
    @SaCheckPermission("sys.manage.role")
    public Result<?> unit() {
        List<Sys_unit> list;
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            Cnd cnd = Cnd.NEW();
            cnd.and(Cnd.exps("type", "=", SysUnitType.GROUP.value()).or("type", "=", SysUnitType.COMPANY.value()));
            cnd.asc("path");
            list = sysUnitService.query(cnd);
        } else {
            String unitId = StpUtil.getSession().getString("unitId");
            String unitPath = sysUnitService.getMasterCompanyPath(unitId);
            Cnd cnd = Cnd.NEW();
            cnd.and("path", "like", unitPath + "%");
            cnd.and(Cnd.exps("type", "=", SysUnitType.GROUP.value()).or("type", "=", SysUnitType.COMPANY.value()));
            cnd.asc("path");
            list = sysUnitService.query(cnd);
        }
        return Result.data(list);
    }

    @GetMapping("/group")
    @Operation(summary = "获取角色组")
    @SaCheckPermission("sys.manage.role")
    public Result<?> group(
            @Parameter(description = "单位ID") @RequestParam(required = false) String unitId) {
        if (!StpUtil.hasPermission("sys.manage.role.system") && Strings.isBlank(unitId)) {
            return Result.error("没有权限");
        }
        return Result.data(sysGroupService.query(Cnd.where("unitId", "=", unitId).asc("createdAt"), "roles"));
    }

    @GetMapping("/post")
    @Operation(summary = "获取职务列表")
    public Result<?> post() {
        return Result.data(sysPostService.query());
    }

    @GetMapping("/app")
    @Operation(summary = "获取APP列表")
    @SaCheckPermission("sys.manage.role")
    public Result<?> app() {
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            return Result.data(sysAppService.query(Cnd.NEW().asc("location")));
        } else {
            return Result.data(sysRoleService.getAppList(StpUtil.getLoginIdAsString()).stream()
                    .filter(app -> !"COMMON".equals(app.getId()))
                    .toList());
        }
    }

    @PostMapping("/create_role")
    @Operation(summary = "新建角色")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.role.create")
    public Result<?> createRole(@RequestBody @Validated SysRoleDTO dto) {
        int num = sysRoleService.count(Cnd.where("code", "=", dto.getCode().trim()));
        if (num > 0) {
            return Result.error("角色代码已存在");
        }
        Sys_role role = new Sys_role();
        BeanUtils.copyProperties(dto, role);
        role.setUnitId(Strings.sNull(dto.getUnitId()));
        role.setCreatedBy(StpUtil.getLoginIdAsString());
        sysRoleService.insert(role);
        return Result.success();
    }

    @PostMapping("/create_group")
    @Operation(summary = "新建角色组")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.role.create")
    public Result<?> createGroup(@RequestBody @Validated SysGroupDTO dto) {
        Sys_group group = new Sys_group();
        BeanUtils.copyProperties(dto, group);
        group.setCreatedBy(StpUtil.getLoginIdAsString());
        group.setUnitPath(Strings.isBlank(dto.getUnitId()) ? "" : sysUnitService.fetch(dto.getUnitId()).getPath());
        sysGroupService.insert(group);
        return Result.success();
    }

    @PostMapping("/update_role")
    @Operation(summary = "修改角色")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.role.update")
    public Result<?> updateRole(@RequestBody @Validated({Default.class, Update.class}) SysRoleDTO dto) {
        int num = sysRoleService.count(Cnd.where("code", "=", dto.getCode().trim()).and("id", "<>", dto.getId()));
        if (num > 0) {
            return Result.error("角色代码已存在");
        }
        Sys_role role = new Sys_role();
        BeanUtils.copyProperties(dto, role);
        role.setUnitId(Strings.sNull(dto.getUnitId()));
        role.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysRoleService.updateIgnoreNull(role);
        sysUserService.cacheClear();
        return Result.success();
    }

    @PostMapping("/update_group")
    @Operation(summary = "修改角色组")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.role.update")
    public Result<?> updateGroup(@RequestBody @Validated({Default.class, Update.class}) SysGroupDTO dto) {
        Sys_group group = new Sys_group();
        BeanUtils.copyProperties(dto, group);
        group.setUnitId(Strings.sNull(dto.getUnitId()));
        group.setUpdatedBy(StpUtil.getLoginIdAsString());
        group.setUnitPath(Strings.isBlank(dto.getUnitId()) ? "" : sysUnitService.fetch(dto.getUnitId()).getPath());
        sysGroupService.updateIgnoreNull(group);
        return Result.success();
    }

    @PostMapping("/delete_role")
    @Operation(summary = "删除角色")
    @SaCheckPermission("sys.manage.role.delete")
    public Result<?> deleteRole(
            @Parameter(description = "角色ID") @RequestParam String id) {
        Sys_role role = sysRoleService.fetch(id);
        if (role == null) {
            return Result.error("数据不存在");
        }
        if (isSysadminRole(role.getCode())) {
            return Result.error("超级管理员角色 不可被删除");
        }
        if (isPublicRole(role.getCode())) {
            return Result.error("公共角色 不可被删除");
        }
        sysRoleService.clearRole(id);
        return Result.success();
    }

    @PostMapping("/delete_group")
    @Operation(summary = "删除角色组")
    @SaCheckPermission("sys.manage.role.delete")
    public Result<?> deleteGroup(
            @Parameter(description = "角色组ID") @RequestParam String id) {
        Sys_group group = sysGroupService.fetch(id);
        if (group == null) {
            return Result.error("数据不存在");
        }
        if ("PUBLIC".equals(group.getId()) || "SYSTEM".equals(group.getId())) {
            return Result.error("系统内置角色组 不可被删除");
        }
        sysGroupService.clearGroup(id);
        return Result.success();
    }

    @PostMapping("/user")
    @Operation(summary = "获取角色用户列表")
    @SaCheckPermission("sys.manage.role")
    public Result<?> user(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        return Result.data(sysRoleService.getUserListPage(roleId, username, pageNo, pageSize, pageOrderName, pageOrderBy));
    }

    @PostMapping("/select_user")
    @Operation(summary = "获取待分配用户列表(排除已分配)")
    @SaCheckPermission("sys.manage.role")
    public Result<?> getSelectUser(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "单位ID") @RequestParam String unitId,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        if (!StpUtil.hasPermission("sys.manage.role.system") && Strings.isBlank(unitId)) {
            return Result.error("没有权限");
        }
        return Result.data(sysRoleService.getSelUserListPage(roleId, username, StpUtil.hasRole("sysadmin"), unitId, pageNo, pageSize, pageOrderName, pageOrderBy));
    }

    @PostMapping("/link_user")
    @Operation(summary = "关联用户到角色")
    @SaCheckPermission("sys.manage.role.user")
    public Result<?> doLinkUser(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "用户ID数组,逗号分隔") @RequestParam String ids) {
        String unitId = StpUtil.getSession().getString("unitId");
        sysRoleService.doLinkUser(roleId, unitId, Strings.splitIgnoreBlank(ids));
        return Result.success();
    }

    @PostMapping("/unlink_user")
    @Operation(summary = "从角色移除用户")
    @SaCheckPermission("sys.manage.role.user")
    public Result<?> doUnLinkUser(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "用户ID") @RequestParam String id) {
        sysRoleService.doLinkUser(roleId, id);
        return Result.success();
    }

    @GetMapping("/get_menus")
    @Operation(summary = "Vue3获取菜单及权限")
    @SaCheckPermission("sys.manage.role")
    public Result<?> getMenuS(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "应用ID") @RequestParam String appId) {
        List<Sys_menu> hasList = sysRoleService.getMenusAndDatas(roleId, appId);
        List<Sys_menu> list;
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            list = sysMenuService.query(Cnd.where("appId", "=", appId).asc("location").asc("path"));
        } else {
            list = sysUserService.getMenusAndDatas(StpUtil.getLoginIdAsString(), appId);
        }
        List<String> menuIds = new ArrayList<>();
        for (Sys_menu menu : hasList) {
            menuIds.add(menu.getId());
        }
        return Result.data(NutMap.NEW().addv("menuList", list).addv("menuIds", menuIds));
    }

    @GetMapping("/get_do_menu")
    @Operation(summary = "Vue2获取菜单及权限")
    @SaCheckPermission("sys.manage.role")
    public Result<?> getDoMenu(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "应用ID") @RequestParam String appId) {
        List<Sys_menu> hasList = sysRoleService.getMenusAndDatas(roleId, appId);
        List<Sys_menu> list;
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            list = sysMenuService.query(Cnd.where("appId", "=", appId).asc("location").asc("path"));
        } else {
            list = sysUserService.getMenusAndDatas(StpUtil.getLoginIdAsString(), appId);
        }
        NutMap menuMap = NutMap.NEW();
        for (Sys_menu menu : list) {
            List<Sys_menu> list1 = menuMap.getList(menu.getParentId(), Sys_menu.class);
            if (list1 == null) {
                list1 = new ArrayList<>();
            }
            list1.add(menu);
            menuMap.put(menu.getParentId(), list1);
        }
        List<String> menuIds = new ArrayList<>();
        for (Sys_menu menu : hasList) {
            menuIds.add(menu.getId());
        }
        return Result.data(NutMap.NEW().addv("menuTree", buildTree(menuMap, "")).addv("menuIds", menuIds));
    }

    private List<NutMap> buildTree(NutMap menuMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_menu> subList = menuMap.getList(pid, Sys_menu.class);
        for (Sys_menu menu : subList) {
            NutMap map = Lang.obj2nutmap(menu);
            map.put("label", menu.getName());
            if (menu.isHasChildren() || (menuMap.get(menu.getId()) != null)) {
                map.put("children", buildTree(menuMap, menu.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @PostMapping("/do_menu")
    @Operation(summary = "为角色分配权限")
    @SaCheckPermission("sys.manage.role.menu")
    public Result<?> doMenu(
            @Parameter(description = "角色ID") @RequestParam String roleId,
            @Parameter(description = "角色代码") @RequestParam String roleCode,
            @Parameter(description = "应用ID") @RequestParam String appId,
            @Parameter(description = "菜单权限ID数组,逗号分隔") @RequestParam(required = false) String menuIds) {
        String[] ids = Strings.splitIgnoreBlank(menuIds);
        if (isSysadminRole(roleCode) && GlobalConstant.DEFAULT_PLATFORM_APPID.equals(appId) && ids.length == 0) {
            return Result.error("超级管理员角色权限不可为空,可能会造成无法登录系统");
        }
        sysRoleService.saveMenu(roleId, appId, ids);
        return Result.success();
    }

    private boolean isSysadminRole(String roleCode) {
        return Strings.isNotBlank(roleCode) && (GlobalConstant.DEFAULT_SYSADMIN_ROLECODE.equals(roleCode) || roleCode.startsWith(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE + ":"));
    }

    private boolean isPublicRole(String roleCode) {
        return Strings.isNotBlank(roleCode) && ("public".equals(roleCode) || roleCode.startsWith("public:"));
    }
}
