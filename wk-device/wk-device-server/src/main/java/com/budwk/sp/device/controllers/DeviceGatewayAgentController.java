package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceGatewayAgentDTO;
import com.budwk.sp.device.dto.DeviceGatewayNodeDTO;
import com.budwk.sp.device.services.DeviceGatewayService;
import com.budwk.sp.device.services.DeviceRuntimeService;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nutz.lang.Strings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iot/gateway/agent")
@SLog(tag = "网关Agent")
@Tag(name = "网关Agent", description = "网关Agent节点接口")
public class DeviceGatewayAgentController {
    private final DeviceRuntimeService deviceRuntimeService;
    private final DeviceGatewayService deviceGatewayService;

    public DeviceGatewayAgentController(DeviceRuntimeService deviceRuntimeService, DeviceGatewayService deviceGatewayService) {
        this.deviceRuntimeService = deviceRuntimeService;
        this.deviceGatewayService = deviceGatewayService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取网关Agent基础数据")
    @SaCheckPermission("iot.manage.gateway.agent")
    public Result<?> data() {
        Map<String, Object> map = new HashMap<>();
        map.put("agentStatuses", List.of(
                Map.of("text", "运行中", "value", "RUNNING"),
                Map.of("text", "空闲", "value", "IDLE"),
                Map.of("text", "已停止", "value", "STOPPED")
        ));
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "查询网关Agent节点")
    @SaCheckPermission("iot.manage.gateway.agent")
    public Result<?> list(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        List<DeviceGatewayAgentDTO> agents = deviceRuntimeService.listGatewayAgents(tenantId).stream()
                .filter(item -> filter(item, keyword, status))
                .toList();
        int claimedGatewayCount = agents.stream().mapToInt(item -> item.getClaimedGatewayCount() == null ? 0 : item.getClaimedGatewayCount()).sum();
        int totalGatewayCount = deviceGatewayService.listEnabled(tenantId, null).size();
        Map<String, Object> map = new HashMap<>();
        map.put("list", agents);
        map.put("totalCount", agents.size());
        map.put("agentCount", agents.size());
        map.put("claimedGatewayCount", claimedGatewayCount);
        map.put("unclaimedGatewayCount", Math.max(totalGatewayCount - claimedGatewayCount, 0));
        map.put("totalGatewayCount", totalGatewayCount);
        return Result.data(map);
    }

    private boolean filter(DeviceGatewayAgentDTO agent, String keyword, String status) {
        if (Strings.isNotBlank(status) && !status.equalsIgnoreCase(agent.getStatus())) {
            return false;
        }
        if (Strings.isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        if (Strings.sNull(agent.getAgentId()).toLowerCase().contains(normalized) || Strings.sNull(agent.getHost()).toLowerCase().contains(normalized)) {
            return true;
        }
        for (DeviceGatewayNodeDTO gateway : agent.getGateways()) {
            if (Strings.sNull(gateway.getName()).toLowerCase().contains(normalized)
                    || Strings.sNull(gateway.getProtocolName()).toLowerCase().contains(normalized)
                    || Strings.sNull(gateway.getGatewayId()).toLowerCase().contains(normalized)) {
                return true;
            }
        }
        return false;
    }
}
