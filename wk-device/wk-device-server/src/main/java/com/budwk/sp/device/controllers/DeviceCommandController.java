package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceCommandDTO;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.services.DeviceCommandService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iot/command")
@SLog(tag = "设备指令")
@Tag(name = "设备指令", description = "设备指令接口")
public class DeviceCommandController {
    private final DeviceCommandService deviceCommandService;
    private final DeviceMessageTemplate deviceMessageTemplate;

    public DeviceCommandController(DeviceCommandService deviceCommandService, DeviceMessageTemplate deviceMessageTemplate) {
        this.deviceCommandService = deviceCommandService;
        this.deviceMessageTemplate = deviceMessageTemplate;
    }

    @GetMapping("/data")
    @Operation(summary = "获取指令基础数据")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("statuses", List.of(DeviceCommandStatus.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("topics", deviceMessageTemplate.getStandardTopics());
        map.put("messageProviders", deviceMessageTemplate.getSupportedProviders());
        map.put("messagePatterns", deviceMessageTemplate.getSupportedPatterns());
        return Result.data(map);
    }

    @PostMapping("/create")
    @Operation(summary = "创建设备指令")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> create(@RequestBody @Validated DeviceCommandDTO dto) { return Result.data(deviceCommandService.createCommands(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @DeleteMapping("/cancel/{id}")
    @Operation(summary = "取消设备指令")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> cancel(@PathVariable String id) { deviceCommandService.cancelCommand(id, StpUtil.getSession().getString("tenantId"), StpUtil.getLoginIdAsString()); return Result.success(); }

    @PostMapping("/retry/{id}")
    @Operation(summary = "重试下发设备指令")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> retry(@PathVariable String id) { return Result.data(deviceCommandService.retryCommand(id, StpUtil.getSession().getString("tenantId"), StpUtil.getLoginIdAsString())); }

    @GetMapping("/pending")
    @Operation(summary = "查询待下发指令")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> pending(@RequestParam String deviceId) { return Result.data(deviceCommandService.listPending(deviceId, StpUtil.getSession().getString("tenantId"))); }

    @GetMapping("/logs")
    @Operation(summary = "查询指令历史")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> logs(@RequestParam String deviceId) { return Result.data(deviceCommandService.listLogs(deviceId, StpUtil.getSession().getString("tenantId"))); }
}
