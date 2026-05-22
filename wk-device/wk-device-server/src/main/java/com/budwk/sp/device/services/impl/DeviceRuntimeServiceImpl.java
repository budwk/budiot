package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceGatewayAgentDTO;
import com.budwk.sp.device.dto.DeviceGatewayNodeDTO;
import com.budwk.sp.device.dto.DeviceRuntimeDTO;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.services.DeviceRuntimeService;
import com.budwk.sp.device.services.DeviceProtocolService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceRuntimeServiceImpl implements DeviceRuntimeService {
    private final StringRedisTemplate stringRedisTemplate;
    private final Dao dao;
    private final DeviceProtocolService deviceProtocolService;

    public DeviceRuntimeServiceImpl(StringRedisTemplate stringRedisTemplate, Dao dao, DeviceProtocolService deviceProtocolService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.dao = dao;
        this.deviceProtocolService = deviceProtocolService;
    }

    @Override
    public DeviceRuntimeDTO getRuntime(String tenantId, String deviceId) {
        DeviceRuntimeDTO runtime = new DeviceRuntimeDTO();
        runtime.setDeviceId(deviceId);
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(deviceRuntimeKey(tenantId, deviceId));
        if (map.isEmpty()) return runtime;
        runtime.setOnline("true".equals(String.valueOf(map.getOrDefault("online", "false"))));
        runtime.setIp(String.valueOf(map.getOrDefault("ip", "")));
        runtime.setGatewayNodeId(String.valueOf(map.getOrDefault("gatewayNodeId", "")));
        runtime.setLastHeartbeatAt(parseLong(map.get("lastHeartbeatAt")));
        runtime.setLastDeviceAt(parseLong(map.get("lastDeviceAt")));
        return runtime;
    }

    @Override
    public List<DeviceGatewayNodeDTO> listGatewayNodes(String tenantId) {
        List<String> nodeIds = stringRedisTemplate.opsForList().range(gatewayNodeListKey(tenantId), 0, -1);
        List<DeviceGatewayNodeDTO> list = new ArrayList<>();
        if (nodeIds == null) return list;
        for (String nodeId : nodeIds) {
            if (Strings.isBlank(nodeId)) continue;
            Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(gatewayNodeKey(tenantId, nodeId));
            if (map.isEmpty()) continue;
            DeviceGatewayNodeDTO dto = new DeviceGatewayNodeDTO();
            dto.setNodeId(nodeId);
            dto.setProtocol(String.valueOf(map.getOrDefault("protocol", "")));
            dto.setHost(String.valueOf(map.getOrDefault("host", "")));
            dto.setPort(parseInt(map.get("port")));
            dto.setLastSeenAt(parseLong(map.get("lastSeenAt")));
            dto.setStatus(String.valueOf(map.getOrDefault("status", "RUNNING")));
            dto.setGatewayProcessId(String.valueOf(map.getOrDefault("gatewayProcessId", "")));
            dto.setClaimed(Strings.isNotBlank(dto.getGatewayProcessId()));
            list.add(dto);
        }
        return list;
    }

    @Override
    public List<DeviceGatewayAgentDTO> listGatewayAgents(String tenantId) {
        List<Device_gateway> gateways = dao.query(Device_gateway.class, Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false));
        Map<String, DeviceGatewayAgentDTO> agentMap = new LinkedHashMap<>();
        for (Device_gateway gateway : gateways) {
            DeviceGatewayNodeDTO node = toGatewayNode(gateway);
            if (!node.isClaimed() || Strings.isBlank(node.getGatewayProcessId())) {
                continue;
            }
            DeviceGatewayAgentDTO agent = agentMap.computeIfAbsent(node.getGatewayProcessId(), this::loadAgent);
            agent.getGateways().add(node);
            agent.setClaimedGatewayCount(agent.getGateways().size());
            if (agent.getLastSeenAt() == null || (node.getLastSeenAt() != null && node.getLastSeenAt() > agent.getLastSeenAt())) {
                agent.setLastSeenAt(node.getLastSeenAt());
            }
        }
        return agentMap.values().stream().peek(item -> item.getGateways().sort(Comparator.comparing(DeviceGatewayNodeDTO::getLastSeenAt, Comparator.nullsLast(Long::compareTo)).reversed()))
                .sorted(Comparator.comparing(DeviceGatewayAgentDTO::getLastSeenAt, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    private DeviceGatewayNodeDTO toGatewayNode(Device_gateway gateway) {
        DeviceGatewayNodeDTO dto = new DeviceGatewayNodeDTO();
        dto.setGatewayId(gateway.getId());
        dto.setNodeId(gateway.getId());
        dto.setName(gateway.getName());
        dto.setProtocol(gateway.getNetworkProtocol() == null ? "" : gateway.getNetworkProtocol().getValue());
        dto.setGatewayMode(gateway.getGatewayMode() == null ? "" : gateway.getGatewayMode().getValue());
        dto.setProtocolId(gateway.getProtocolId());
        dto.setHost(gateway.getHost());
        dto.setPort(gateway.getPort());
        dto.setPath(gateway.getPath());
        dto.setRemoteHost(gateway.getRemoteHost());
        dto.setRemotePort(gateway.getRemotePort());
        Map<Object, Object> runtime = stringRedisTemplate.opsForHash().entries(gatewayRuntimeKey(gateway.getTenantId(), gateway.getId()));
        if (!runtime.isEmpty()) {
            dto.setLastSeenAt(parseLong(runtime.get("lastSeenAt")));
            dto.setLastError(String.valueOf(runtime.getOrDefault("lastError", "")));
            dto.setGatewayProcessId(String.valueOf(runtime.getOrDefault("gatewayProcessId", "")));
            dto.setClaimed(Strings.isNotBlank(dto.getGatewayProcessId()));
            String status = String.valueOf(runtime.getOrDefault("status", gateway.getRuntimeStatus() == null ? "" : gateway.getRuntimeStatus().name()));
            dto.setStatus(status);
        } else {
            dto.setStatus(gateway.getRuntimeStatus() == null ? DeviceGatewayStatus.STOPPED.getValue() : gateway.getRuntimeStatus().getValue());
        }
        if (Strings.isNotBlank(gateway.getProtocolId())) {
            try {
                Device_protocol protocol = deviceProtocolService.getProtocol(gateway.getProtocolId(), gateway.getTenantId());
                dto.setProtocolName(protocol.getName());
            } catch (Exception ignored) {
            }
        }
        if (!dto.isClaimed()) {
            String owner = stringRedisTemplate.opsForValue().get(gatewayOwnerKey(gateway.getTenantId(), gateway.getId()));
            dto.setGatewayProcessId(Strings.sNull(owner).trim());
            dto.setClaimed(Strings.isNotBlank(dto.getGatewayProcessId()));
        }
        return dto;
    }

    private DeviceGatewayAgentDTO loadAgent(String agentId) {
        DeviceGatewayAgentDTO dto = new DeviceGatewayAgentDTO();
        dto.setAgentId(agentId);
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(gatewayAgentKey(agentId));
        dto.setHost(String.valueOf(map.getOrDefault("host", "")));
        dto.setStatus(String.valueOf(map.getOrDefault("status", "RUNNING")));
        dto.setLastSeenAt(parseLong(map.get("lastSeenAt")));
        return dto;
    }

    private String deviceRuntimeKey(String tenantId, String deviceId) { return "wk:device:runtime:" + tenantId + ":" + deviceId; }
    private String gatewayNodeListKey(String tenantId) { return "wk:device:gateway:nodes:" + tenantId; }
    private String gatewayNodeKey(String tenantId, String nodeId) { return "wk:device:gateway:node:" + tenantId + ":" + nodeId; }
    private String gatewayRuntimeKey(String tenantId, String gatewayId) { return "wk:device:gateway:runtime:" + tenantId + ":" + gatewayId; }
    private String gatewayOwnerKey(String tenantId, String gatewayId) { return "wk:device:gateway:owner:" + tenantId + ":" + gatewayId; }
    private String gatewayAgentKey(String agentId) { return "wk:device:gateway:agent:" + agentId; }
    private Long parseLong(Object value) { try { return value == null ? null : Long.parseLong(String.valueOf(value)); } catch (Exception e) { return null; } }
    private Integer parseInt(Object value) { try { return value == null ? null : Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return null; } }
}
