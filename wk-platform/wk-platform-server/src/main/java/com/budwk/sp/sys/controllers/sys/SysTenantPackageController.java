package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysTenantPackageDTO;
import com.budwk.sp.sys.entity.Sys_tenant_package;
import com.budwk.sp.sys.services.SysTenantPackageService;
import com.budwk.sp.sys.services.SysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sys/tenant/package")
@SLog(tag = "租户套餐管理")
@Tag(name = "租户套餐管理", description = "租户套餐管理接口")
public class SysTenantPackageController {
    private final SysTenantPackageService sysTenantPackageService;
    private final SysTenantService sysTenantService;

    public SysTenantPackageController(SysTenantPackageService sysTenantPackageService, SysTenantService sysTenantService) {
        this.sysTenantPackageService = sysTenantPackageService;
        this.sysTenantService = sysTenantService;
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询租户套餐")
    @SaCheckPermission("sys.manage.tenant.package")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (disabled != null) {
            cnd.and("disabled", "=", disabled);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        } else {
            cnd.desc("createdAt");
        }
        return Result.data(sysTenantPackageService.listPage(pageNo, pageSize, cnd));
    }

    @GetMapping("/data")
    @Operation(summary = "获取租户套餐配置数据")
    @SaCheckPermission("sys.manage.tenant.package")
    public Result<?> data() {
        return Result.data(NutMap.NEW().addv("menuTree", sysTenantPackageService.getMenuTree()));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取租户套餐详情")
    @SaCheckPermission("sys.manage.tenant.package")
    public Result<?> getData(@PathVariable String id) {
        Sys_tenant_package tenantPackage = sysTenantPackageService.fetch(id);
        if (tenantPackage == null) {
            return Result.error("数据不存在");
        }
        return Result.data(NutMap.NEW().addv("package", tenantPackage).addv("menuIds", sysTenantPackageService.getMenuIds(id)));
    }

    @PostMapping("/create")
    @Operation(summary = "创建租户套餐")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.tenant.package.create")
    public Result<?> create(@RequestBody SysTenantPackageDTO dto) {
        if (sysTenantPackageService.count(Cnd.where("name", "=", Strings.sNull(dto.getName()).trim())) > 0) {
            return Result.error("套餐名称已存在");
        }
        Sys_tenant_package tenantPackage = new Sys_tenant_package();
        BeanUtils.copyProperties(dto, tenantPackage);
        sysTenantPackageService.savePackage(tenantPackage, dto.getMenuIds(), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改租户套餐")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.tenant.package.update")
    public Result<?> update(@RequestBody @org.springframework.validation.annotation.Validated({Default.class, Update.class}) SysTenantPackageDTO dto) {
        if (sysTenantPackageService.count(Cnd.where("name", "=", Strings.sNull(dto.getName()).trim()).and("id", "<>", dto.getId())) > 0) {
            return Result.error("套餐名称已存在");
        }
        Sys_tenant_package tenantPackage = new Sys_tenant_package();
        BeanUtils.copyProperties(dto, tenantPackage);
        sysTenantPackageService.updatePackage(tenantPackage, dto.getMenuIds(), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用租户套餐")
    @SaCheckPermission("sys.manage.tenant.package.update")
    public Result<?> changeDisabled(@Parameter(description = "ID") @RequestParam String id,
                                    @Parameter(description = "true=禁用") @RequestParam boolean disabled) {
        int res = sysTenantPackageService.update(org.nutz.dao.Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        return res > 0 ? Result.success() : Result.error();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除租户套餐")
    @SaCheckPermission("sys.manage.tenant.package.delete")
    public Result<?> delete(@PathVariable String id) {
        if (sysTenantService.count(Cnd.where("packageId", "=", id)) > 0) {
            return Result.error("已有租户使用该套餐，不允许删除");
        }
        sysTenantPackageService.delete(id);
        sysTenantPackageService.clear("sys_tenant_package_menu", Cnd.where("packageId", "=", id));
        return Result.success();
    }
}
