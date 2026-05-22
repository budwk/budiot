package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysConfigDTO;
import com.budwk.sp.sys.entity.Sys_config;
import com.budwk.sp.sys.enums.SysConfigType;
import com.budwk.sp.sys.services.SysAppService;
import com.budwk.sp.sys.services.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/param")
@SLog(tag = "系统参数")
@Tag(name = "系统参数", description = "系统参数配置接口")
public class SysParamController {

    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private SysAppService sysAppService;

    @GetMapping("/data")
    @Operation(summary = "获取配置数据")
    @SaCheckPermission("sys.config.param")
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("apps", sysAppService.listAll());
        map.put("types", SysConfigType.values());
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询")
    @SaCheckPermission("sys.config.param")
    public Result<?> list(
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "参数Key") @RequestParam(required = false) String configKey,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (StringUtils.hasText(appId)) {
            cnd.and("appId", "=", appId);
        }
        if (StringUtils.hasText(configKey)) {
            cnd.and("configKey", "like", "%" + configKey + "%");
        }
        if (StringUtils.hasText(pageOrderName) && StringUtils.hasText(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(sysConfigService.listPage(pageNo, pageSize, cnd));
    }

    @PostMapping("/create")
    @Operation(summary = "新增参数")
    @RepeatSubmit
    @SaCheckPermission("sys.config.param.create")
    public Result<?> create(@RequestBody @Validated SysConfigDTO dto) {
        Sys_config config = new Sys_config();
        BeanUtils.copyProperties(dto, config);
        config.setCreatedBy(StpUtil.getLoginIdAsString());
        if (sysConfigService.count(Cnd.where("configKey", "=", config.getConfigKey())
                .and("appId", "=", GlobalConstant.DEFAULT_COMMON_APPID)) > 0) {
            return Result.error("新增参数 " + config.getConfigKey() + " 与公共参数重复");
        }
        sysConfigService.insert(config);
        sysConfigService.cacheClear();
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改参数")
    @RepeatSubmit
    @SaCheckPermission("sys.config.param.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysConfigDTO dto) {
        Sys_config config = new Sys_config();
        BeanUtils.copyProperties(dto, config);
        config.setUpdatedBy(StpUtil.getLoginIdAsString());
        if (sysConfigService.count(Cnd.where("configKey", "=", config.getConfigKey())
                .and("appId", "=", GlobalConstant.DEFAULT_COMMON_APPID)
                .and("id", "<>", config.getId())) > 0) {
            return Result.error("参数 " + config.getConfigKey() + " 与公共参数重复");
        }
        sysConfigService.updateIgnoreNull(config);
        sysConfigService.cacheClear();
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取参数")
    @SaCheckPermission("sys.config.param")
    public Result<?> getData(@Parameter(description = "ID") @PathVariable String id) {
        Sys_config config = sysConfigService.fetch(id);
        if (config == null) {
            return Result.error("数据不存在");
        }
        return Result.data(config);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除参数")
    @SaCheckPermission("sys.config.param.delete")
    public Result<?> delete(@Parameter(description = "ID") @PathVariable String id) {
        Sys_config config = sysConfigService.fetch(id);
        if (config == null) {
            return Result.error("数据不存在");
        }
        if (GlobalConstant.DEFAULT_COMMON_APPID.equalsIgnoreCase(config.getAppId())
                && config.getConfigKey().startsWith("App")) {
            return Result.error("系统内置公共参数不可删除");
        }
        sysConfigService.delete(id);
        sysConfigService.cacheClear();
        return Result.success();
    }
}
