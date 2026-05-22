package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.services.SysKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/key")
@SLog(tag = "密钥管理")
@Tag(name = "密钥管理", description = "密钥管理接口")
public class SysKeyController {

    @Autowired
    private SysKeyService sysKeyService;

    @PostMapping("/list")
    @Operation(summary = "分页查询")
    @SaCheckPermission("sys.config.key")
    public Result<?> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (StringUtils.hasText(pageOrderName) && StringUtils.hasText(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(sysKeyService.listPage(pageNo, pageSize, cnd));
    }

    @PostMapping("/create")
    @Operation(summary = "创建密钥")
    @RepeatSubmit
    @SaCheckPermission("sys.config.key.create")
    public Result<?> create(@Parameter(description = "密钥名称") @RequestParam String name) {
        sysKeyService.createAppkey(name, StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @PostMapping("/disabled")
    @Operation(summary = "启用禁用")
    @RepeatSubmit
    @SaCheckPermission("sys.config.key.update")
    public Result<?> changeDisabled(
            @Parameter(description = "appid") @RequestParam String appid,
            @Parameter(description = "是否禁用") @RequestParam boolean disabled) {
        sysKeyService.updateAppkey(appid, disabled, StpUtil.getLoginIdAsString());
        return Result.success();
    }

    @DeleteMapping("/delete/{appid}")
    @Operation(summary = "删除密钥")
    @SaCheckPermission("sys.config.key.delete")
    public Result<?> delete(@Parameter(description = "appid") @PathVariable String appid) {
        sysKeyService.deleteAppkey(appid);
        return Result.success();
    }
}
