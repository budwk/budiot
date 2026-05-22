package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysMenuDTO;
import com.budwk.sp.sys.entity.Sys_menu;
import com.budwk.sp.sys.services.SysAppService;
import com.budwk.sp.sys.services.SysMenuService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.json.Json;
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
@RequestMapping("/sys/menu")
@SLog(tag = "菜单管理")
@Tag(name = "菜单管理", description = "菜单管理接口")
public class SysMenuController {
    @Autowired
    private SysMenuService sysMenuService;
    @Autowired
    private SysAppService sysAppService;
    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/data")
    @Operation(summary = "获取配置数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> data() {
        NutMap map = NutMap.NEW();
        map.addv("apps", sysAppService.listAll());
        return Result.data(map);
    }

    @GetMapping("/list")
    @Operation(summary = "Vue3树形列表查询")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> list(
            @Parameter(description = "菜单名称") @RequestParam(required = false) String name,
            @Parameter(description = "所属应用") @RequestParam String appId,
            @Parameter(description = "菜单路径") @RequestParam(required = false) String href) {
        Cnd cnd = Cnd.NEW();
        cnd.and("appId", "=", appId);
        if (StringUtils.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (StringUtils.isNotBlank(href)) {
            cnd.and("href", "like", "%" + href + "%");
        }
        cnd.asc("location");
        cnd.asc("path");
        return Result.success().addData(sysMenuService.query(cnd));
    }

    @GetMapping("/child")
    @Operation(summary = "Vue2获取列表树型数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> getChild(
            @Parameter(description = "父级ID") @RequestParam(required = false) String pid,
            @Parameter(description = "应用ID") @RequestParam String appId) {
        List<Sys_menu> list;
        List<NutMap> treeList = new ArrayList<>();
        Cnd cnd = Cnd.NEW();
        if (Strings.isBlank(pid)) {
            cnd.and(Cnd.exps("parentId", "=", "").or("parentId", "is", null));
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.and("appId", "=", appId);
        cnd.asc("location").asc("path");
        list = sysMenuService.query(cnd);
        for (Sys_menu menu : list) {
            NutMap map = Lang.obj2nutmap(menu);
            map.addv("expanded", false);
            map.addv("children", new ArrayList<>());
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取待选择树型数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> getTree(
            @Parameter(description = "父级ID") @RequestParam(required = false) String pid,
            @Parameter(description = "应用ID") @RequestParam String appId) {
        List<NutMap> treeList = new ArrayList<>();
        if (Strings.isBlank(pid)) {
            NutMap root = NutMap.NEW().addv("value", "root").addv("label", "默认顶级").addv("leaf", true);
            treeList.add(root);
        }
        Cnd cnd = Cnd.NEW();
        if (Strings.isBlank(pid)) {
            cnd.and(Cnd.exps("parentId", "=", "").or("parentId", "is", null));
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.and("appId", "=", appId);
        cnd.and("type", "=", "menu");
        cnd.asc("location").asc("path");
        List<Sys_menu> list = sysMenuService.query(cnd);
        for (Sys_menu menu : list) {
            NutMap map = NutMap.NEW().addv("value", menu.getId()).addv("label", menu.getName());
            if (menu.isHasChildren()) {
                map.addv("children", new ArrayList<>());
                map.addv("leaf", false);
            } else {
                map.addv("leaf", true);
            }
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @PostMapping("/create")
    @Operation(summary = "创建菜单")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.menu.create")
    public Result<?> create(@RequestBody NutMap nutMap) {
        Sys_menu sysMenu = nutMap.getAs("menu", Sys_menu.class);
        String appId = nutMap.getString("appId");
        List<NutMap> buttons = Json.fromJsonAsList(NutMap.class, nutMap.getString("buttons"));
        int num = sysMenuService.count(Cnd.where("permission", "=", sysMenu.getPermission().trim()));
        if (num > 0) {
            return Result.error("权限标识已存在");
        }
        for (NutMap map : buttons) {
            num = sysMenuService.count(Cnd.where("permission", "=", map.getString("permission", "").trim()));
            if (num > 0) {
                return Result.error("权限标识已存在");
            }
        }
        String parentId = sysMenu.getParentId();
        if ("root".equals(sysMenu.getParentId())) {
            parentId = "";
        }
        sysMenu.setHasChildren(false);
        sysMenu.setCreatedBy(StpUtil.getLoginIdAsString());
        sysMenuService.save(appId, sysMenu, Strings.sNull(parentId), buttons);
        sysUserService.cacheClear();
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用菜单")
    @SaCheckPermission("sys.manage.menu.update")
    public Result<?> changeDisabled(
            @Parameter(description = "主键ID") @RequestParam String id,
            @Parameter(description = "PATH路径") @RequestParam(required = false) String path,
            @Parameter(description = "true=禁用") @RequestParam boolean disabled) {
        if (Strings.sNull(path).startsWith("0001")) {
            return Result.error("系统菜单禁止操作");
        }
        int res = sysMenuService.update(Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        if (res > 0) {
            sysUserService.cacheClear();
            return Result.success();
        }
        return Result.error();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除菜单")
    @SaCheckPermission("sys.manage.menu.delete")
    public Result<?> delete(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_menu menu = sysMenuService.fetch(id);
        if (menu == null) {
            return Result.error("数据不存在");
        }
        if (!GlobalConstant.DEFAULT_SYSADMIN_LOGINNAME.equals(StpUtil.getSession().getString("loginname"))
                && Strings.sNull(menu.getPath()).startsWith("0001")) {
            return Result.error("系统菜单禁止操作");
        }
        sysMenuService.deleteAndChild(menu);
        return Result.success();
    }

    @GetMapping("/get_sort_tree")
    @Operation(summary = "获取待排序数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> getSortTree(
            @Parameter(description = "应用ID") @RequestParam String appId) {
        List<Sys_menu> list = sysMenuService.query(Cnd.where("appId", "=", appId).asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Sys_menu menu : list) {
            List<Sys_menu> list1 = nutMap.getList(menu.getParentId(), Sys_menu.class);
            if (list1 == null) {
                list1 = new ArrayList<>();
            }
            list1.add(menu);
            nutMap.put(Strings.sNull(menu.getParentId()), list1);
        }
        return Result.data(buildTree(nutMap, ""));
    }

    private List<NutMap> buildTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_menu> subList = nutMap.getList(pid, Sys_menu.class);
        for (Sys_menu menu : subList) {
            NutMap map = Lang.obj2nutmap(menu);
            map.put("label", menu.getName());
            if (menu.isHasChildren() || (nutMap.get(menu.getId()) != null)) {
                map.put("children", buildTree(nutMap, menu.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @PostMapping("/sort")
    @Operation(summary = "菜单排序")
    @SaCheckPermission("sys.manage.menu.update")
    public Result<?> sortDo(
            @Parameter(description = "ID数组,逗号分隔") @RequestParam String ids,
            @Parameter(description = "应用ID") @RequestParam String appId) {
        String[] menuIds = StringUtils.split(ids, ",");
        int i = 0;
        sysMenuService.update(Chain.make("location", 0), Cnd.where("appId", "=", appId));
        for (String id : menuIds) {
            if (StringUtils.isNotBlank(id)) {
                sysMenuService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        sysUserService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get_menu/{id}")
    @Operation(summary = "获取菜单数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> getMenu(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_menu menu = sysMenuService.fetch(id);
        NutMap map = Lang.obj2nutmap(menu);
        map.put("parentName", "无");
        map.put("children", "false");
        if (Strings.isNotBlank(menu.getParentId())) {
            map.put("parentName", sysMenuService.fetch(menu.getParentId()).getName());
        }
        List<Sys_menu> list = sysMenuService.query(Cnd.where("parentId", "=", id).and("type", "=", "data").asc("location").asc("path"));
        List<NutMap> buttons = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            map.put("children", "true");
            for (Sys_menu m : list) {
                buttons.add(NutMap.NEW().addv("key", m.getId()).addv("name", m.getName()).addv("permission", m.getPermission()));
            }
        }
        map.put("buttons", buttons);
        return Result.data(map);
    }

    @PostMapping("/update_menu")
    @Operation(summary = "修改菜单")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.menu.update")
    public Result<?> updateMenu(@RequestBody NutMap nutMap) {
        Sys_menu sysMenu = nutMap.getAs("menu", Sys_menu.class);
        List<NutMap> buttons = Json.fromJsonAsList(NutMap.class, nutMap.getString("buttons"));
        int num = sysMenuService.count(Cnd.where("permission", "=", sysMenu.getPermission().trim()).and("id", "<>", sysMenu.getId()));
        if (num > 0) {
            return Result.error("权限标识已存在");
        }
        for (NutMap map : buttons) {
            num = sysMenuService.count(Cnd.where("permission", "=", map.getString("permission", "").trim()).and("id", "<>", map.getString("key", "")));
            if (num > 0) {
                return Result.error("权限标识已存在");
            }
        }
        sysMenu.setHasChildren(false);
        sysMenu.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysMenuService.edit(sysMenu, sysMenu.getParentId(), buttons);
        sysUserService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get_data/{id}")
    @Operation(summary = "获取权限数据")
    @SaCheckPermission("sys.manage.menu")
    public Result<?> getData(@Parameter(description = "主键ID") @PathVariable String id) {
        return Result.data(sysMenuService.fetch(id));
    }

    @PostMapping("/update_data")
    @Operation(summary = "修改权限")
    @SaCheckPermission("sys.manage.menu.update")
    public Result<?> updateData(@RequestBody @Validated({Default.class, Update.class}) SysMenuDTO dto) {
        Sys_menu menu = new Sys_menu();
        BeanUtils.copyProperties(dto, menu);
        int num = sysMenuService.count(Cnd.where("permission", "=", menu.getPermission().trim()).and("id", "<>", menu.getId()));
        if (num > 0) {
            return Result.error("权限标识已存在");
        }
        sysMenuService.updateIgnoreNull(menu);
        sysUserService.cacheClear();
        return Result.success();
    }
}
