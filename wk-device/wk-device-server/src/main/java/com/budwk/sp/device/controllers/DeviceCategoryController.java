package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceCategoryDTO;
import com.budwk.sp.device.entity.Device_category;
import com.budwk.sp.device.services.DeviceCategoryService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/iot/category")
@SLog(tag = "设备分类")
@Tag(name = "设备分类", description = "设备分类接口")
public class DeviceCategoryController {
    private final DeviceCategoryService deviceCategoryService;

    public DeviceCategoryController(DeviceCategoryService deviceCategoryService) { this.deviceCategoryService = deviceCategoryService; }

    @GetMapping("/list")
    @Operation(summary = "树形列表查询")
    @SaCheckPermission("iot.manage.category")
    public Result<?> list(@RequestParam(required = false) String name) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (StringUtils.hasText(name)) cnd.and("name", "like", "%" + name + "%");
        cnd.asc("location").asc("path");
        return Result.data(deviceCategoryService.query(cnd));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取待选择树型数据")
    @SaCheckPermission("iot.manage.category")
    public Result<?> tree(@RequestParam(required = false) String pid) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        List<NutMap> treeList = new ArrayList<>();
        if (!StringUtils.hasText(pid)) treeList.add(NutMap.NEW().addv("value", "root").addv("label", "默认顶级").addv("leaf", true));
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (!StringUtils.hasText(pid)) cnd.and("parentId", "=", "").or("parentId", "is", null);
        else cnd.and("parentId", "=", pid);
        cnd.asc("location").asc("path");
        List<Device_category> list = deviceCategoryService.query(cnd);
        for (Device_category category : list) {
            NutMap map = NutMap.NEW().addv("value", category.getId()).addv("label", category.getName());
            map.addv("leaf", !category.isHasChildren());
            if (category.isHasChildren()) map.addv("children", new ArrayList<>());
            treeList.add(map);
        }
        return Result.data(treeList);
    }

    @PostMapping("/create")
    @Operation(summary = "新增分类")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.category.create")
    public Result<?> create(@RequestBody @Validated DeviceCategoryDTO dto) {
        Device_category category = new Device_category();
        BeanUtils.copyProperties(dto, category);
        category.setCreatedBy(StpUtil.getLoginIdAsString());
        String parentId = "root".equals(dto.getParentId()) ? "" : dto.getParentId();
        deviceCategoryService.save(category, parentId, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取分类详情")
    @SaCheckPermission("iot.manage.category")
    public Result<?> get(@PathVariable String id) { return Result.data(deviceCategoryService.getCategory(id, StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/update")
    @Operation(summary = "修改分类")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.category.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceCategoryDTO dto) {
        Device_category category = deviceCategoryService.getCategory(dto.getId(), StpUtil.getSession().getString("tenantId"));
        category.setName(dto.getName());
        category.setCode(dto.getCode());
        category.setDisabled(dto.isDisabled());
        category.setUpdatedBy(StpUtil.getLoginIdAsString());
        deviceCategoryService.updateIgnoreNull(category);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类")
    @SaCheckPermission("iot.manage.category.delete")
    public Result<?> delete(@PathVariable String id) {
        Device_category category = deviceCategoryService.getCategory(id, StpUtil.getSession().getString("tenantId"));
        deviceCategoryService.deleteAndChild(category, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }

    @GetMapping("/get_sort_tree")
    @Operation(summary = "获取待排序数据")
    @SaCheckPermission("iot.manage.category")
    public Result<?> getSortTree() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        List<Device_category> list = deviceCategoryService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false).asc("location").asc("path"));
        NutMap nutMap = NutMap.NEW();
        for (Device_category category : list) {
            List<Device_category> subList = nutMap.getList(category.getParentId(), Device_category.class);
            if (subList == null) subList = new ArrayList<>();
            subList.add(category);
            nutMap.put(Strings.sNull(category.getParentId()), subList);
        }
        return Result.data(buildTree(nutMap, ""));
    }

    @PostMapping("/sort")
    @Operation(summary = "保存排序数据")
    @SaCheckPermission("iot.manage.category.update")
    public Result<?> sort(@RequestParam String ids) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        String[] idArray = ids.split(",");
        int i = 0;
        deviceCategoryService.update(Chain.make("location", 0), Cnd.where("tenantId", "=", tenantId));
        for (String id : idArray) {
            if (StringUtils.hasText(id)) {
                deviceCategoryService.update(Chain.make("location", i), Cnd.where("tenantId", "=", tenantId).and("id", "=", id));
                i++;
            }
        }
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @SaCheckPermission("iot.manage.category.update")
    public Result<?> disabled(@RequestParam String id, @RequestParam boolean disabled) {
        int res = deviceCategoryService.update(Chain.make("disabled", disabled), Cnd.where("tenantId", "=", StpUtil.getSession().getString("tenantId")).and("id", "=", id));
        return res > 0 ? Result.success() : Result.error();
    }

    private List<NutMap> buildTree(NutMap nutMap, String pid) {
        List<NutMap> treeList = new ArrayList<>();
        List<Device_category> subList = nutMap.getList(pid, Device_category.class);
        if (subList == null) return treeList;
        for (Device_category category : subList) {
            NutMap map = Lang.obj2nutmap(category);
            map.put("label", category.getName());
            if (category.isHasChildren() || nutMap.get(category.getId()) != null) map.put("children", buildTree(nutMap, category.getId()));
            treeList.add(map);
        }
        return treeList;
    }
}
