package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceGatewayDTO;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.services.DeviceGatewayService;
import com.budwk.sp.device.services.DeviceProtocolService;
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
@RequestMapping("/iot/gateway")
@SLog(tag = "设备网关")
@Tag(name = "设备网关", description = "设备网关接口")
public class DeviceGatewayController {
    private final DeviceGatewayService deviceGatewayService;
    private final DeviceProtocolService deviceProtocolService;

    public DeviceGatewayController(DeviceGatewayService deviceGatewayService, DeviceProtocolService deviceProtocolService) {
        this.deviceGatewayService = deviceGatewayService;
        this.deviceProtocolService = deviceProtocolService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取网关基础数据")
    @SaCheckPermission("iot.manage.gateway")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Map<String, Object> map = new HashMap<>();
        map.put("networkProtocols", List.of(DeviceNetworkProtocol.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("gatewayModes", List.of(DeviceGatewayMode.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("runtimeStatuses", List.of(DeviceGatewayStatus.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("protocols", deviceProtocolService.listEnabled(tenantId));
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询网关")
    @SaCheckPermission("iot.manage.gateway")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String networkProtocol,
                          @RequestParam(required = false) String runtimeStatus,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(name)) cnd.and("name", "like", "%" + name + "%");
        if (Strings.isNotBlank(networkProtocol)) cnd.and("networkProtocol", "=", networkProtocol);
        if (Strings.isNotBlank(runtimeStatus)) cnd.and("runtimeStatus", "=", runtimeStatus);
        if (disabled != null) cnd.and("disabled", "=", disabled);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        else cnd.desc("updatedAt");
        var page = deviceGatewayService.listPage(pageNo, pageSize, cnd);
        enrich(page.getList(), tenantId);
        return Result.data(page);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取网关详情")
    @SaCheckPermission("iot.manage.gateway")
    public Result<?> get(@PathVariable String id) {
        return Result.data(deviceGatewayService.getGateway(id, StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/create")
    @Operation(summary = "新增网关")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.gateway.create")
    public Result<?> create(@RequestBody @Validated DeviceGatewayDTO dto) {
        return Result.data(deviceGatewayService.createGateway(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/update")
    @Operation(summary = "修改网关")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.gateway.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceGatewayDTO dto) {
        return Result.data(deviceGatewayService.updateGateway(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @PostMapping("/start/{id}")
    @Operation(summary = "启动网关")
    @SaCheckPermission("iot.manage.gateway.update")
    public Result<?> start(@PathVariable String id) {
        return Result.data(deviceGatewayService.changeStatus(id, StpUtil.getSession().getString("tenantId"), DeviceGatewayStatus.RUNNING, StpUtil.getLoginIdAsString()));
    }

    @PostMapping("/suspend/{id}")
    @Operation(summary = "挂起网关")
    @SaCheckPermission("iot.manage.gateway.update")
    public Result<?> suspend(@PathVariable String id) {
        return Result.data(deviceGatewayService.changeStatus(id, StpUtil.getSession().getString("tenantId"), DeviceGatewayStatus.SUSPENDED, StpUtil.getLoginIdAsString()));
    }

    @PostMapping("/stop/{id}")
    @Operation(summary = "停止网关")
    @SaCheckPermission("iot.manage.gateway.update")
    public Result<?> stop(@PathVariable String id) {
        return Result.data(deviceGatewayService.changeStatus(id, StpUtil.getSession().getString("tenantId"), DeviceGatewayStatus.STOPPED, StpUtil.getLoginIdAsString()));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除网关")
    @SaCheckPermission("iot.manage.gateway.delete")
    public Result<?> delete(@PathVariable String id) {
        deviceGatewayService.deleteGateway(id, StpUtil.getSession().getString("tenantId"));
        return Result.success();
    }

    private void enrich(List<?> rows, String tenantId) {
        if (rows == null || rows.isEmpty()) return;
        List<Device_gateway> list = rows.stream().filter(Device_gateway.class::isInstance).map(Device_gateway.class::cast).toList();
        if (list.isEmpty()) return;
        for (Device_gateway gateway : list) {
            Device_gateway data = deviceGatewayService.getGateway(gateway.getId(), tenantId);
            gateway.setProtocol(data.getProtocol());
            gateway.setRuntimeStatus(data.getRuntimeStatus());
            gateway.setLastSeenAt(data.getLastSeenAt());
            gateway.setLastError(data.getLastError());
            gateway.setLastStartedAt(data.getLastStartedAt());
        }
    }
}
