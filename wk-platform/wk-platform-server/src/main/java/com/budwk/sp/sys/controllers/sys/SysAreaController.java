package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysAreaDTO;
import com.budwk.sp.sys.entity.Sys_area;
import com.budwk.sp.sys.services.SysAreaService;
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
@RequestMapping("/sys/area")
@SLog(tag = "行政区划")
@Tag(name = "行政区划", description = "行政区划管理接口")
public class SysAreaController {

    @Autowired
    private SysAreaService sysAreaService;

    @GetMapping("/list")
    @Operation(summary = "树形列表查询")
    @SaCheckPermission("sys.config.area")
    public Result<?> list() {
        Cnd cnd = Cnd.NEW();
        cnd.asc("location");
        cnd.asc("path");
        return Result.data(sysAreaService.query(cnd));
    }

    @GetMapping("/child")
    @Operation(summary = "获取列表树型数据")
    @SaCheckPermission("sys.config.area")
    public Result<?> getChild(@Parameter(description = "父级ID") @RequestParam(required = false) String pid) {
        List<NutMap> treeList = new ArrayList<>();
        Cnd cnd = Cnd.NEW();
        if (!StringUtils.hasText(pid)) {
            cnd.and("parentId", "=", "").or("parentId", "is", null);
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.asc("location").asc("path");
        List<Sys_area> list = sysAreaService.query(cnd);
        for (Sys_area area : list) {
            NutMap map = Lang.obj2nutmap(area);
            map.addv("expanded", false);
            map.addv("children", new ArrayList<>());
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取待选择树型数据")
    @SaCheckPermission("sys.config.area")
    public Result<?> getTree(@Parameter(description = "父级ID") @RequestParam(required = false) String pid) {
        List<NutMap> treeList = new ArrayList<>();
        if (!StringUtils.hasText(pid)) {
            NutMap root = NutMap.NEW().addv("value", "root").addv("label", "默认顶级").addv("leaf", true);
            treeList.add(root);
        }
        Cnd cnd = Cnd.NEW();
        if (!StringUtils.hasText(pid)) {
            cnd.and("parentId", "=", "").or("parentId", "is", null);
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.asc("location").asc("path");
        List<Sys_area> list = sysAreaService.query(cnd);
        for (Sys_area area : list) {
            NutMap map = NutMap.NEW().addv("value", area.getId()).addv("label", area.getName());
            if (area.isHasChildren()) {
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
    @Operation(summary = "新增区划")
    @RepeatSubmit
    @SaCheckPermission("sys.config.area.create")
    public Result<?> create(@RequestBody @Validated SysAreaDTO dto) {
        Sys_area area = new Sys_area();
        BeanUtils.copyProperties(dto, area);
        area.setCreatedBy(StpUtil.getLoginIdAsString());
        String parentId = dto.getParentId();
        if ("root".equals(parentId) || parentId == null) {
            parentId = "";
        }
        sysAreaService.save(area, parentId);
        sysAreaService.cacheClear();
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除区划")
    @SaCheckPermission("sys.config.area.delete")
    public Result<?> delete(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_area area = sysAreaService.fetch(id);
        if (area == null) {
            return Result.error("数据不存在");
        }
        sysAreaService.deleteAndChild(area);
        sysAreaService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取区划")
    public Result<?> getData(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_area area = sysAreaService.fetch(id);
        if (area == null) {
            return Result.error("数据不存在");
        }
        return Result.data(area);
    }

    @PostMapping("/update")
    @Operation(summary = "修改区划")
    @RepeatSubmit
    @SaCheckPermission("sys.config.area.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysAreaDTO dto) {
        Sys_area area = new Sys_area();
        BeanUtils.copyProperties(dto, area);
        area.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysAreaService.updateIgnoreNull(area);
        sysAreaService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get_sort_tree")
    @Operation(summary = "获取待排序数据")
    @SaCheckPermission("sys.config.area")
    public Result<?> getSortTree() {
        List<Sys_area> list = sysAreaService.query(Cnd.NEW().asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Sys_area area : list) {
            List<Sys_area> subList = nutMap.getList(area.getParentId(), Sys_area.class);
            if (subList == null) {
                subList = new ArrayList<>();
            }
            subList.add(area);
            nutMap.put(Strings.sNull(area.getParentId()), subList);
        }
        return Result.data(buildTree(nutMap, ""));
    }

    private List<NutMap> buildTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_area> subList = nutMap.getList(pid, Sys_area.class);
        if (subList == null) {
            return treeList;
        }
        for (Sys_area area : subList) {
            NutMap map = Lang.obj2nutmap(area);
            map.put("label", area.getName());
            if (area.isHasChildren() || (nutMap.get(area.getId()) != null)) {
                map.put("children", buildTree(nutMap, area.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @PostMapping("/sort")
    @Operation(summary = "保存排序数据")
    @SaCheckPermission("sys.config.area.update")
    public Result<?> sortDo(@Parameter(description = "ids数组,逗号分隔") @RequestParam String ids) {
        String[] idArray = ids.split(",");
        int i = 0;
        sysAreaService.update(Chain.make("location", 0), Cnd.NEW());
        for (String id : idArray) {
            if (StringUtils.hasText(id)) {
                sysAreaService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        sysAreaService.cacheClear();
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @SaCheckPermission("sys.config.area.update")
    public Result<?> changeDisabled(
            @Parameter(description = "主键ID") @RequestParam String id,
            @Parameter(description = "是否禁用") @RequestParam boolean disabled) {
        int res = sysAreaService.update(Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        if (res > 0) {
            sysAreaService.cacheClear();
            return Result.success();
        }
        return Result.error();
    }
}
