package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysDictDTO;
import com.budwk.sp.sys.entity.Sys_dict;
import com.budwk.sp.sys.services.SysDictService;
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
@RequestMapping("/sys/dict")
@SLog(tag = "数据字典")
@Tag(name = "数据字典", description = "数据字典管理接口")
public class SysDictController {

    @Autowired
    private SysDictService sysDictService;

    @GetMapping("/list")
    @Operation(summary = "树形列表查询")
    @SaCheckPermission("sys.config.dict")
    public Result<?> list(@Parameter(description = "字典名称") @RequestParam(required = false) String name) {
        Cnd cnd = Cnd.NEW();
        if (StringUtils.hasText(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        cnd.asc("location");
        cnd.asc("path");
        return Result.data(sysDictService.query(cnd));
    }

    @GetMapping("/child")
    @Operation(summary = "获取列表树型数据")
    @SaCheckPermission("sys.config.dict")
    public Result<?> getChild(@Parameter(description = "父级ID") @RequestParam(required = false) String pid) {
        List<NutMap> treeList = new ArrayList<>();
        Cnd cnd = Cnd.NEW();
        if (!StringUtils.hasText(pid)) {
            cnd.and("parentId", "=", "").or("parentId", "is", null);
        } else {
            cnd.and("parentId", "=", pid);
        }
        cnd.asc("location").asc("path");
        List<Sys_dict> list = sysDictService.query(cnd);
        for (Sys_dict dict : list) {
            NutMap map = Lang.obj2nutmap(dict);
            map.addv("expanded", false);
            map.addv("children", new ArrayList<>());
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取待选择树型数据")
    @SaCheckPermission("sys.config.dict")
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
        List<Sys_dict> list = sysDictService.query(cnd);
        for (Sys_dict dict : list) {
            NutMap map = NutMap.NEW().addv("value", dict.getId()).addv("label", dict.getName());
            if (dict.isHasChildren()) {
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
    @Operation(summary = "新增字典项")
    @RepeatSubmit
    @SaCheckPermission("sys.config.dict.create")
    public Result<?> create(@RequestBody @Validated SysDictDTO dto) {
        Sys_dict dict = new Sys_dict();
        BeanUtils.copyProperties(dto, dict);
        dict.setCreatedBy(StpUtil.getLoginIdAsString());
        String parentId = dto.getParentId();
        if ("root".equals(parentId) || parentId == null) {
            parentId = "";
        }
        sysDictService.save(dict, parentId);
        sysDictService.cacheClear();
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除字典项")
    @SaCheckPermission("sys.config.dict.delete")
    public Result<?> delete(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_dict dict = sysDictService.fetch(id);
        if (dict == null) {
            return Result.error("数据不存在");
        }
        sysDictService.deleteAndChild(dict);
        sysDictService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取字典项")
    public Result<?> getData(@Parameter(description = "主键ID") @PathVariable String id) {
        Sys_dict dict = sysDictService.fetch(id);
        if (dict == null) {
            return Result.error("数据不存在");
        }
        return Result.data(dict);
    }

    @PostMapping("/update")
    @Operation(summary = "修改字典项")@RepeatSubmit
    @SaCheckPermission("sys.config.dict.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysDictDTO dto) {
        Sys_dict dict = new Sys_dict();
        BeanUtils.copyProperties(dto, dict);
        dict.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysDictService.updateIgnoreNull(dict);
        sysDictService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get_sort_tree")
    @Operation(summary = "获取待排序数据")
    @SaCheckPermission("sys.config.dict")
    public Result<?> getSortTree() {
        List<Sys_dict> list = sysDictService.query(Cnd.NEW().asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Sys_dict dict : list) {
            List<Sys_dict> subList = nutMap.getList(dict.getParentId(), Sys_dict.class);
            if (subList == null) {
                subList = new ArrayList<>();
            }
            subList.add(dict);
            nutMap.put(Strings.sNull(dict.getParentId()), subList);
        }
        return Result.data(buildTree(nutMap, ""));
    }

    private List<NutMap> buildTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Sys_dict> subList = nutMap.getList(pid, Sys_dict.class);
        if (subList == null) {
            return treeList;
        }
        for (Sys_dict dict : subList) {
            NutMap map = Lang.obj2nutmap(dict);
            map.put("label", dict.getName());
            if (dict.isHasChildren() || (nutMap.get(dict.getId()) != null)) {
                map.put("children", buildTree(nutMap, dict.getId()));
            }
            treeList.add(map);
        }
        return treeList;
    }

    @PostMapping("/sort")
    @Operation(summary = "保存排序数据")
    @SaCheckPermission("sys.config.dict.update")
    public Result<?> sortDo(@Parameter(description = "ids数组,逗号分隔") @RequestParam String ids) {
        String[] idArray = ids.split(",");
        int i = 0;
        sysDictService.update(Chain.make("location", 0), Cnd.NEW());
        for (String id : idArray) {
            if (StringUtils.hasText(id)) {
                sysDictService.update(Chain.make("location", i), Cnd.where("id", "=", id));
                i++;
            }
        }
        sysDictService.cacheClear();
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @SaCheckPermission("sys.config.dict.update")
    public Result<?> changeDisabled(
            @Parameter(description = "主键ID") @RequestParam String id,
            @Parameter(description = "是否禁用") @RequestParam boolean disabled) {
        int res = sysDictService.update(Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        if (res > 0) {
            sysDictService.cacheClear();
            return Result.success();
        }
        return Result.error();
    }
}
