package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.sys.enums.SysLogType;
import com.budwk.sp.sys.services.SysAppService;
import com.budwk.sp.sys.services.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统日志控制器
 *
 * @author wizzer@qq.com
 */
@RestController
@RequestMapping("/sys/log")
@SLog(tag = "系统日志")
@Tag(name = "系统日志", description = "系统日志管理接口")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;
    @Autowired
    private SysAppService sysAppService;

    @GetMapping("/data")
    @Operation(summary = "获取日志查询数据")
    @SaCheckLogin
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("apps", sysAppService.listAll());
        map.put("types", List.of(SysLogType.values()).stream().map(type -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", type.getText());
            item.put("value", type.getValue());
            return item;
        }).toList());
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "获取日志列表")
    @SaCheckPermission("sys.manage.log")
    public Result<?> list(
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "日志类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "用户名") @RequestParam(required = false) String loginname,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "功能模块") @RequestParam(required = false) String tag,
            @Parameter(description = "日志内容") @RequestParam(required = false) String msg,
            @Parameter(description = "开始时间") @RequestParam(required = false) Long beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Long endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        return Result.data(sysLogService.getLogPage(appId, type, status, loginname, username, tag, msg,
                beginTime, endTime, pageNo, pageSize, pageOrderName, pageOrderBy));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除日志")
    @SaCheckPermission("sys.manage.log.delete")
    public Result<?> delete(@Parameter(description = "日志ID") @PathVariable String id,
                            @Parameter(description = "日志创建时间") @RequestParam Long createdAt) {
        if (!sysLogService.deleteLog(id, createdAt)) {
            return Result.error("日志不存在");
        }
        return Result.success();
    }

    @PostMapping("/clear")
    @Operation(summary = "清空日志")
    @SaCheckPermission("sys.manage.log.delete")
    public Result<?> clear(
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "日志类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "用户名") @RequestParam(required = false) String loginname,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String username,
            @Parameter(description = "功能模块") @RequestParam(required = false) String tag,
            @Parameter(description = "日志内容") @RequestParam(required = false) String msg,
            @Parameter(description = "开始时间") @RequestParam(required = false) Long beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Long endTime) {
        int count = sysLogService.clearLogs(appId, type, status, loginname, username, tag, msg, beginTime, endTime);
        return Result.success("清理成功，共处理 " + count + " 条日志");
    }
}
