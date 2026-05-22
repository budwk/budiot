package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolDTO;
import com.budwk.sp.device.enums.DeviceProtocolScriptType;
import com.budwk.sp.device.services.DeviceProtocolService;
import com.budwk.sp.device.support.DeviceProtocolScriptSamples;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iot/protocol")
@SLog(tag = "设备协议")
@Tag(name = "设备协议", description = "设备协议配置接口")
public class DeviceProtocolController {
    private final DeviceProtocolService deviceProtocolService;

    public DeviceProtocolController(DeviceProtocolService deviceProtocolService) { this.deviceProtocolService = deviceProtocolService; }

    @GetMapping("/data")
    @Operation(summary = "获取协议基础数据")
    @SaCheckPermission("iot.manage.protocol")
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("scriptTypes", List.of(DeviceProtocolScriptType.values()).stream().map(type -> {
            Map<String, String> item = new HashMap<>();
            item.put("text", type.getText());
            item.put("value", type.getValue());
            return item;
        }).toList());
        map.put("sampleScript", DeviceProtocolScriptSamples.sampleScript());
        map.put("sampleInputJson", DeviceProtocolScriptSamples.sampleInputJson());
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询协议")
    @SaCheckPermission("iot.manage.protocol")
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
        return Result.data(deviceProtocolService.listPage(pageNo, pageSize, cnd));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取协议详情")
    @SaCheckPermission("iot.manage.protocol")
    public Result<?> get(@PathVariable String id) { return Result.data(deviceProtocolService.getProtocol(id, StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/create")
    @Operation(summary = "新增协议")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.protocol.create")
    public Result<?> create(@RequestBody @Validated DeviceProtocolDTO dto) { return Result.data(deviceProtocolService.createProtocol(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/update")
    @Operation(summary = "修改协议")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.protocol.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceProtocolDTO dto) { return Result.data(deviceProtocolService.updateProtocol(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/debug")
    @Operation(summary = "调试协议脚本")
    @SaCheckPermission("iot.manage.protocol")
    public Result<?> debug(@RequestBody @Validated DeviceProtocolDebugRequestDTO dto) {
        return Result.data(deviceProtocolService.debug(dto, StpUtil.getSession().getString("tenantId")));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除协议")
    @SaCheckPermission("iot.manage.protocol.delete")
    public Result<?> delete(@PathVariable String id) { deviceProtocolService.deleteProtocol(id, StpUtil.getSession().getString("tenantId")); return Result.success(); }
}
