package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.services.DeviceDashboardService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iot/dashboard")
@SLog(tag = "设备看板")
@Tag(name = "设备看板", description = "设备看板接口")
public class DeviceDashboardController {
    private final DeviceDashboardService deviceDashboardService;

    public DeviceDashboardController(DeviceDashboardService deviceDashboardService) {
        this.deviceDashboardService = deviceDashboardService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取IoT看板数据")
    @SaCheckPermission("iot.manage.dashboard")
    public Result<?> data(@RequestParam(required = false, defaultValue = "today") String rangeType,
                          @RequestParam(required = false) Long startAt,
                          @RequestParam(required = false) Long endAt) {
        return Result.data(deviceDashboardService.getDashboardData(StpUtil.getSession().getString("tenantId"), rangeType, startAt, endAt));
    }
}
