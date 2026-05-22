package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysTenantDTO;
import com.budwk.sp.sys.dto.SysTenantDisabledDTO;
import com.budwk.sp.sys.dto.SysTenantExpireDTO;
import com.budwk.sp.sys.entity.Sys_tenant;
import com.budwk.sp.sys.entity.Sys_tenant_package;
import com.budwk.sp.sys.services.SysTenantPackageService;
import com.budwk.sp.sys.services.SysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/tenant")
@SLog(tag = "租户管理")
@Tag(name = "租户管理", description = "租户管理接口")
public class SysTenantController {
    private final SysTenantService sysTenantService;
    private final SysTenantPackageService sysTenantPackageService;

    public SysTenantController(SysTenantService sysTenantService, SysTenantPackageService sysTenantPackageService) {
        this.sysTenantService = sysTenantService;
        this.sysTenantPackageService = sysTenantPackageService;
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询租户")
    @SaCheckPermission("sys.manage.tenant")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String packageId,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        if (Strings.isNotBlank(packageId)) {
            cnd.and("packageId", "=", packageId);
        }
        if (disabled != null) {
            cnd.and("disabled", "=", disabled);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        } else {
            cnd.desc("createdAt");
        }
        var page = sysTenantService.listPage(pageNo, pageSize, cnd);
        Map<String, String> packageMap = new HashMap<>();
        List<Sys_tenant_package> packages = sysTenantPackageService.query(Cnd.NEW());
        for (Sys_tenant_package tenantPackage : packages) {
            packageMap.put(tenantPackage.getId(), tenantPackage.getName());
        }
        for (Object obj : page.getList()) {
            if (obj instanceof Sys_tenant tenant) {
                tenant.setTenantPackage(new Sys_tenant_package());
                tenant.getTenantPackage().setId(tenant.getPackageId());
                tenant.getTenantPackage().setName(packageMap.get(tenant.getPackageId()));
            }
        }
        return Result.data(page);
    }

    @GetMapping("/data")
    @Operation(summary = "获取租户配置数据")
    @SaCheckPermission("sys.manage.tenant")
    public Result<?> data() {
        return Result.data(NutMap.NEW().addv("packages", sysTenantPackageService.query(Cnd.where("disabled", "=", false).asc("createdAt"))));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取租户详情")
    @SaCheckPermission("sys.manage.tenant")
    public Result<?> getData(@PathVariable String id) {
        return Result.data(sysTenantService.getTenant(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建租户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.tenant.create")
    public Result<?> create(@RequestBody SysTenantDTO dto) {
        sysTenantService.createTenant(dto, StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改租户")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.tenant.update")
    public Result<?> update(@RequestBody @org.springframework.validation.annotation.Validated({Default.class, Update.class}) SysTenantDTO dto) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(dto.getId())) {
            return Result.error("默认平台租户不允许修改");
        }
        sysTenantService.updateTenant(dto, StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用租户")
    @SaCheckPermission("sys.manage.tenant.update")
    public Result<?> changeDisabled(@RequestBody @org.springframework.validation.annotation.Validated SysTenantDisabledDTO dto) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(dto.getId())) {
            return Result.error("默认平台租户不允许操作");
        }
        sysTenantService.changeDisabled(dto.getId(), dto.isDisabled(), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/expire")
    @Operation(summary = "修改租户到期时间")
    @SaCheckPermission("sys.manage.tenant.update")
    public Result<?> expire(@RequestBody @org.springframework.validation.annotation.Validated SysTenantExpireDTO dto) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(dto.getId())) {
            return Result.error("默认平台租户不允许操作");
        }
        sysTenantService.updateExpire(dto.getId(), dto.isHasExpire(), dto.getExpireAt(), StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除租户")
    @SaCheckPermission("sys.manage.tenant.delete")
    public Result<?> delete(@PathVariable String id) {
        if (GlobalConstant.TENANT_ID_DEFAULT.equals(id)) {
            return Result.error("默认平台租户不允许删除");
        }
        sysTenantService.deleteTenant(id);
        return Result.success();
    }
}
