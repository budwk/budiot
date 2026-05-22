package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysUnitDTO;
import com.budwk.sp.sys.entity.Sys_unit;
import com.budwk.sp.sys.entity.Sys_unit_user;
import com.budwk.sp.sys.services.SysUnitService;
import com.budwk.sp.sys.services.SysUnitUserService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/unit")
@SLog(tag = "单位管理")
@Tag(name = "单位管理", description = "单位管理接口")
public class SysUnitController {

    @Autowired
    private SysUnitService sysUnitService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUnitUserService sysUnitUserService;

    @GetMapping("/list")
    @Operation(summary = "列表查询")
    @SaCheckPermission("sys.manage.unit")
    public Result<?> list(
            @Parameter(description = "单位名称") @RequestParam(required = false) String name,
            @Parameter(description = "部门负责人") @RequestParam(required = false) String leaderName) {
        Cnd cnd = Cnd.NEW();
        if (StringUtils.hasText(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (StringUtils.hasText(leaderName)) {
            cnd.and("leaderName", "like", "%" + leaderName + "%");
        }
        cnd.asc("path");
        return Result.data(sysUnitService.query(cnd));
    }

    @GetMapping("/child")
    @Operation(summary = "获取列表树型数据")
    @SaCheckPermission("sys.manage.unit")
    public Result<?> getChild(@Parameter(description = "父级ID") @RequestParam(required = false) String pid) {
        List<Sys_unit> list;
        List<NutMap> treeList = new ArrayList<>();
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            Cnd cnd = Cnd.NEW();
            if (!StringUtils.hasText(pid)) {
                cnd.and("parentId", "=", "").or("parentId", "is", null);
            } else {
                cnd.and("parentId", "=", pid);
            }
            cnd.asc("location").asc("path");
            list = sysUnitService.query(cnd);
        } else {
            String unitId = StpUtil.getSession().getString("unitId");
            if (!StringUtils.hasText(pid)) {
                list = sysUnitService.query(Cnd.where("id", "=", sysUnitService.getMasterCompanyId(unitId)).asc("location").asc("path"));
            } else {
                Cnd cnd = Cnd.NEW();
                cnd.and("parentId", "=", pid);
                cnd.asc("location").asc("path");
                list = sysUnitService.query(cnd);
            }
        }
        for (Sys_unit unit : list) {
            NutMap map = Lang.obj2nutmap(unit);
            map.addv("expanded", false);
            map.addv("level1", !StringUtils.hasText(pid));
            map.addv("children", new ArrayList<>());
            map.addv("userNumber", sysUserService.count(Cnd.where("unitId", "=", unit.getId())));
            map.addv("allNumber", sysUserService.count(Cnd.where("unitPath", "like", unit.getPath() + "%")));
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @PostMapping("/create")
    @Operation(summary = "新建单位")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.unit.create")
    public Result<?> create(@RequestBody @Validated SysUnitDTO dto) {
        Sys_unit unit = new Sys_unit();
        BeanUtils.copyProperties(dto, unit);
        unit.setCreatedBy(StpUtil.getLoginIdAsString());
        sysUnitService.save(unit);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除单位")
    @SaCheckPermission("sys.manage.unit.delete")
    public Result<?> delete(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_unit unit = sysUnitService.fetch(id);
        if (unit == null) {
            return Result.error("数据不存在");
        }
        if ("0001".equals(Strings.sNull(unit.getPath()))) {
            return Result.error("禁止删除");
        }
        sysUnitService.deleteAndChild(unit);
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取单位信息")
    @SaCheckPermission("sys.manage.unit")
    public Result<?> getData(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_unit unit = sysUnitService.fetch(id);
        if (unit == null) {
            return Result.error("数据不存在");
        }
        List<Sys_unit_user> unitUserList = sysUnitUserService.query(Cnd.where("unitId", "=", id), "user");
        return Result.data(NutMap.NEW().addv("unit", unit).addv("unitUserList", unitUserList));
    }

    @PostMapping("/update")
    @Operation(summary = "修改单位")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.unit.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysUnitDTO dto) {
        Sys_unit unit = new Sys_unit();
        BeanUtils.copyProperties(dto, unit);
        unit.setUpdatedBy(StpUtil.getLoginIdAsString());
        String[] leaders = Strings.splitIgnoreBlank(dto.getLeader());
        String[] highers = Strings.splitIgnoreBlank(dto.getHigher());
        String[] assigners = Strings.splitIgnoreBlank(dto.getAssigner());
        sysUnitService.update(unit, leaders, highers, assigners);
        return Result.success();
    }

    @GetMapping("/get_sort_tree")
    @Operation(summary = "获取待排序数据")
    @SaCheckPermission("sys.manage.unit")
    public Result<?> getSortTree() {
        Cnd cnd = Cnd.NEW();
        if (!StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            String unitId = StpUtil.getSession().getString("unitId");
            Sys_unit unit = sysUnitService.fetch(unitId);
            if (unit != null) {
                cnd.and("path", "like", unit.getPath() + "%");
            }
        }
        cnd.asc("location").asc("path");
        List<Sys_unit> list = sysUnitService.query(cnd);
        NutMap nutMap = NutMap.NEW();
        for (Sys_unit unit : list) {
            List<Sys_unit> subList = nutMap.getList(unit.getParentId(), Sys_unit.class);
            if (subList == null) {
                subList = new ArrayList<>();
            }
            subList.add(unit);
            nutMap.put(Strings.sNull(unit.getParentId()), subList);
        }
        return Result.data(buildTree(nutMap, ""));
    }

    private List<NutMap> buildTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_unit> subList = nutMap.getList(pid, Sys_unit.class);
        if (subList == null) {
            return treeList;
        }
        for (Sys_unit unit : subList) {
            NutMap map = Lang.obj2nutmap(unit);
            map.put("label", unit.getName());
            if (unit.isHasChildren() || (nutMap.get(unit.getId()) != null)) {
                map.put("children", buildTree(nutMap, unit.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @PostMapping("/sort")
    @Operation(summary = "保存排序数据")
    @SaCheckPermission("sys.manage.unit.update")
    public Result<?> sortDo(@Parameter(description = "ids数组,逗号分隔") @RequestParam String ids) {
        String[] idArray = ids.split(",");
        int i = 0;
        if (StpUtil.hasRole(GlobalConstant.DEFAULT_SYSADMIN_ROLECODE)) {
            sysUnitService.update(Chain.make("location", 0), Cnd.NEW());
        }
        for (String id : idArray) {
            if (StringUtils.hasText(id)) {
                sysUnitService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        return Result.success();
    }

    @PostMapping("/search_user")
    @Operation(summary = "查询单位用户")
    @SaCheckPermission("sys.manage.unit")
    public Result<?> searchUser(
            @Parameter(description = "查询关键词") @RequestParam(required = false) String query,
            @Parameter(description = "单位ID") @RequestParam(required = false) String unitId) {
        return Result.data(sysUnitService.searchUser(query, unitId));
    }
}
