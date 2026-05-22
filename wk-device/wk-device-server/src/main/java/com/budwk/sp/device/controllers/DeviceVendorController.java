package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceVendorDTO;
import com.budwk.sp.device.services.DeviceVendorService;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iot/vendor")
@SLog(tag = "设备厂家")
@Tag(name = "设备厂家", description = "设备厂家接口")
public class DeviceVendorController {
    private final DeviceVendorService deviceVendorService;

    public DeviceVendorController(DeviceVendorService deviceVendorService) { this.deviceVendorService = deviceVendorService; }

    @PostMapping("/list")
    @Operation(summary = "分页查询厂家")
    @SaCheckPermission("iot.manage.vendor")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String code,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(name)) cnd.and("name", "like", "%" + name + "%");
        if (Strings.isNotBlank(code)) cnd.and("code", "like", "%" + code + "%");
        if (disabled != null) cnd.and("disabled", "=", disabled);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        else cnd.desc("updatedAt");
        return Result.data(deviceVendorService.listPage(pageNo, pageSize, cnd));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取厂家详情")
    @SaCheckPermission("iot.manage.vendor")
    public Result<?> get(@PathVariable String id) { return Result.data(deviceVendorService.getVendor(id, StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/create")
    @Operation(summary = "新增厂家")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.vendor.create")
    public Result<?> create(@RequestBody @Validated DeviceVendorDTO dto) { return Result.data(deviceVendorService.createVendor(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/update")
    @Operation(summary = "修改厂家")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.vendor.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceVendorDTO dto) { return Result.data(deviceVendorService.updateVendor(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除厂家")
    @SaCheckPermission("iot.manage.vendor.delete")
    public Result<?> delete(@PathVariable String id) { deviceVendorService.deleteVendor(id, StpUtil.getSession().getString("tenantId")); return Result.success(); }
}
