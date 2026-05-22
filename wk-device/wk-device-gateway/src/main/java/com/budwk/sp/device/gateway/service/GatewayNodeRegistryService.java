package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.network.DeviceGatewayBinding;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GatewayNodeRegistryService {
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceGatewayProperties properties;
    private volatile List<DeviceGatewayBinding> bindings = new ArrayList<>();

    public GatewayNodeRegistryService(StringRedisTemplate stringRedisTemplate, DeviceGatewayProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public synchronized void syncBindings(Collection<DeviceGatewayBinding> activeBindings) {
        Map<String, DeviceGatewayBinding> previous = this.bindings.stream().collect(Collectors.toMap(DeviceGatewayBinding::getNodeId, item -> item, (a, b) -> a, HashMap::new));
        this.bindings = new ArrayList<>(activeBindings);
        for (DeviceGatewayBinding binding : activeBindings) {
            String listKey = listKey(binding.getTenantId());
            List<String> exists = stringRedisTemplate.opsForList().range(listKey, 0, -1);
            if (exists == null || !exists.contains(binding.getNodeId())) {
                stringRedisTemplate.opsForList().leftPush(listKey, binding.getNodeId());
            }
            refresh(binding, "RUNNING");
            previous.remove(binding.getNodeId());
        }
        for (DeviceGatewayBinding binding : previous.values()) {
            refresh(binding, "STOPPED");
        }
        refreshProcess(activeBindings.size(), activeBindings.isEmpty() ? "IDLE" : "RUNNING");
    }

    @Scheduled(fixedDelayString = "${wk.device.gateway.heartbeat-seconds:10}000")
    public void heartbeat() {
        refreshProcess(bindings.size(), bindings.isEmpty() ? "IDLE" : "RUNNING");
        for (DeviceGatewayBinding binding : bindings) {
            refresh(binding, "RUNNING");
        }
    }

    public void unregister(Collection<DeviceGatewayBinding> bindings) {
        for (DeviceGatewayBinding binding : bindings) {
            refresh(binding, "STOPPED");
        }
        refreshProcess(0, "STOPPED");
    }

    private void refresh(DeviceGatewayBinding binding, String status) {
        long now = System.currentTimeMillis();
        String processId = "RUNNING".equals(status) ? properties.getNodeId() : "";
        Map<String, String> nodeData = new HashMap<>();
        nodeData.put("nodeId", binding.getNodeId());
        nodeData.put("gatewayId", binding.getGatewayId());
        nodeData.put("name", binding.getNodeId());
        nodeData.put("protocol", binding.getProtocol().getValue());
        nodeData.put("gatewayMode", binding.getGatewayMode() == null ? "" : binding.getGatewayMode().getValue());
        nodeData.put("host", binding.getHost());
        nodeData.put("port", String.valueOf(binding.getPort()));
        nodeData.put("path", binding.getPath() == null ? "" : binding.getPath());
        nodeData.put("remoteHost", binding.getRemoteHost() == null ? "" : binding.getRemoteHost());
        nodeData.put("remotePort", binding.getRemotePort() == null ? "" : String.valueOf(binding.getRemotePort()));
        nodeData.put("protocolId", binding.getProtocolId() == null ? "" : binding.getProtocolId());
        nodeData.put("status", status);
        nodeData.put("lastSeenAt", String.valueOf(now));
        nodeData.put("gatewayProcessId", processId);
        stringRedisTemplate.opsForHash().putAll(hashKey(binding.getTenantId(), binding.getNodeId()), nodeData);

        Map<String, String> runtimeData = new HashMap<>();
        runtimeData.put("status", status);
        runtimeData.put("lastSeenAt", String.valueOf(now));
        runtimeData.put("lastError", "");
        runtimeData.put("gatewayProcessId", processId);
        stringRedisTemplate.opsForHash().putAll(runtimeKey(binding.getTenantId(), binding.getGatewayId()), runtimeData);
    }

    private void refreshProcess(int gatewayCount, String status) {
        long now = System.currentTimeMillis();
        Map<String, String> processData = new HashMap<>();
        processData.put("agentId", properties.getNodeId());
        processData.put("host", properties.getHost());
        processData.put("status", status);
        processData.put("claimedGatewayCount", String.valueOf(gatewayCount));
        processData.put("lastSeenAt", String.valueOf(now));
        stringRedisTemplate.opsForHash().putAll(agentKey(properties.getNodeId()), processData);
    }

    private String listKey(String tenantId) {
        return "wk:device:gateway:nodes:" + tenantId;
    }

    private String hashKey(String tenantId, String nodeId) {
        return "wk:device:gateway:node:" + tenantId + ":" + nodeId;
    }

    private String runtimeKey(String tenantId, String gatewayId) {
        return "wk:device:gateway:runtime:" + tenantId + ":" + gatewayId;
    }

    private String agentKey(String agentId) {
        return "wk:device:gateway:agent:" + agentId;
    }
}
