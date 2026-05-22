package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysAppDTO;
import com.budwk.sp.sys.entity.Sys_app;
import com.budwk.sp.sys.services.SysAppService;
import com.budwk.sp.sys.services.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/app")
@SLog(tag = "应用管理")
@Tag(name = "应用管理", description = "应用管理接口")
public class SysAppController {

    @Autowired
    private SysAppService sysAppService;
    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/list")
    @Operation(summary = "分页查询")
    @SaCheckPermission("sys.manage.app")
    public Result<?> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        return Result.data(sysAppService.listPage(pageNo, pageSize, Cnd.NEW().asc("location")));
    }

    @PostMapping("/create")
    @Operation(summary = "创建应用")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.app.create")
    public Result<?> create(@RequestBody @Validated SysAppDTO dto) {
        Sys_app app = new Sys_app();
        BeanUtils.copyProperties(dto, app);
        app.setCreatedBy(StpUtil.getLoginIdAsString());
        sysAppService.insert(app);
        sysAppService.cacheClear();
        sysUserService.cacheClear();
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改应用")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.app.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysAppDTO dto) {
        if (dto.isDisabled() && GlobalConstant.DEFAULT_COMMON_APPID.equalsIgnoreCase(dto.getId())) {
            return Result.error("COMMON 应用不可禁用");
        }
        if (dto.isDisabled() && GlobalConstant.DEFAULT_PLATFORM_APPID.equalsIgnoreCase(dto.getId())) {
            return Result.error("PLATFORM 应用不可禁用");
        }
        Sys_app app = new Sys_app();
        BeanUtils.copyProperties(dto, app);
        app.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysAppService.updateIgnoreNull(app);
        sysAppService.cacheClear();
        sysUserService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取应用")
    @SaCheckPermission("sys.manage.app")
    public Result<?> getData(@Parameter(description = "应用ID") @PathVariable String id) {
        Sys_app app = sysAppService.fetch(id);
        if (app == null) {
            return Result.error("数据不存在");
        }
        return Result.data(app);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除应用")
    @SaCheckPermission("sys.manage.app.delete")
    public Result<?> delete(@Parameter(description = "应用ID") @PathVariable String id) {
        Sys_app app = sysAppService.fetch(id);
        if (app == null) {
            return Result.error("数据不存在");
        }
        if (GlobalConstant.DEFAULT_COMMON_APPID.equalsIgnoreCase(app.getId())) {
            return Result.error("COMMON 应用不可删除");
        }
        if (GlobalConstant.DEFAULT_PLATFORM_APPID.equalsIgnoreCase(app.getId())) {
            return Result.error("PLATFORM 应用不可删除");
        }
        sysAppService.delete(id);
        sysAppService.cacheClear();
        sysUserService.cacheClear();
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @SaCheckPermission("sys.manage.app.update")
    public Result<?> changeDisabled(
            @Parameter(description = "主键ID") @RequestParam String id,
            @Parameter(description = "是否禁用") @RequestParam boolean disabled) {
        if (GlobalConstant.DEFAULT_COMMON_APPID.equalsIgnoreCase(id)) {
            return Result.error("COMMON 应用不可禁用");
        }
        if (GlobalConstant.DEFAULT_PLATFORM_APPID.equalsIgnoreCase(id)) {
            return Result.error("PLATFORM 应用不可禁用");
        }
        int res = sysAppService.update(Chain.make("disabled", disabled), Cnd.where("id", "=", id));
        sysAppService.cacheClear();
        sysUserService.cacheClear();
        if (res > 0) {
            return Result.success();
        }
        return Result.error();
    }

    @PostMapping("/location")
    @Operation(summary = "修改应用排序")
    @SaCheckPermission("sys.manage.app.update")
    public Result<?> location(
            @Parameter(description = "排序序号") @RequestParam int location,
            @Parameter(description = "主键ID") @RequestParam String id) {
        sysAppService.update(Chain.make("location", location), Cnd.where("id", "=", id));
        sysAppService.cacheClear();
        sysUserService.cacheClear();
        return Result.success();
    }
}
