package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.dto.DeviceAccessDTO;
import com.budwk.sp.device.network.DeviceGatewayBinding;
import com.budwk.sp.device.network.DeviceNetworkInboundMessage;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GatewaySessionService {
    private final StringRedisTemplate stringRedisTemplate;
    private final Set<String> localNodeIds = new HashSet<>();
    private final ConcurrentHashMap<String, String> bindingByDeviceId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> bindingByDeviceCode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceCodeByDeviceId = new ConcurrentHashMap<>();

    public GatewaySessionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void registerLocalBindings(Iterable<DeviceGatewayBinding> bindings) {
        localNodeIds.clear();
        for (DeviceGatewayBinding binding : bindings) {
            localNodeIds.add(binding.getNodeId());
        }
    }

    public void bind(DeviceAccessDTO access, DeviceNetworkInboundMessage message) {
        bindingByDeviceId.put(access.getId(), message.getNodeId());
        bindingByDeviceCode.put(access.getDeviceCode(), message.getNodeId());
        deviceCodeByDeviceId.put(access.getId(), access.getDeviceCode());
        stringRedisTemplate.opsForValue().set(routeKey(access.getTenantId(), access.getId()), message.getNodeId());
        stringRedisTemplate.opsForHash().putAll(runtimeKey(access.getTenantId(), access.getId()), java.util.Map.of(
                "online", "true",
                "tenantId", Strings.sBlank(access.getTenantId(), ""),
                "productId", Strings.sBlank(access.getProductId(), ""),
                "productKey", Strings.sBlank(access.getProductKey(), ""),
                "deviceCode", Strings.sBlank(access.getDeviceCode(), ""),
                "ip", Strings.sBlank(message.getRemoteAddress(), ""),
                "gatewayNodeId", message.getNodeId(),
                "lastHeartbeatAt", String.valueOf(System.currentTimeMillis()),
                "lastDeviceAt", String.valueOf(System.currentTimeMillis())
        ));
    }

    public String resolveBindingNode(String gatewayNodeId, String tenantId, String deviceId, String deviceCode) {
        if (Strings.isNotBlank(gatewayNodeId)) return gatewayNodeId;
        String binding = deviceId != null ? bindingByDeviceId.get(deviceId) : null;
        if (Strings.isBlank(binding) && deviceCode != null) binding = bindingByDeviceCode.get(deviceCode);
        if (Strings.isBlank(binding) && Strings.isNotBlank(tenantId) && Strings.isNotBlank(deviceId)) {
            binding = stringRedisTemplate.opsForValue().get(routeKey(tenantId, deviceId));
        }
        return binding;
    }

    public boolean isLocalNode(String gatewayNodeId) {
        return Strings.isNotBlank(gatewayNodeId) && localNodeIds.contains(gatewayNodeId);
    }

    /**
     * 检查设备是否在线
     */
    public boolean isOnline(String tenantId, String deviceId) {
        if (Strings.isBlank(tenantId) || Strings.isBlank(deviceId)) {
            return false;
        }
        
        // 1. 先检查本地内存缓存
        if (bindingByDeviceId.containsKey(deviceId)) {
            return true;
        }
        
        // 2. 检查 Redis 运行时状态
        try {
            String online = (String) stringRedisTemplate.opsForHash().get(runtimeKey(tenantId, deviceId), "online");
            return "true".equals(online);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更新设备最后心跳时间
     */
    public void updateHeartbeat(String tenantId, String deviceId) {
        if (Strings.isBlank(tenantId) || Strings.isBlank(deviceId)) {
            return;
        }
        try {
            stringRedisTemplate.opsForHash().put(runtimeKey(tenantId, deviceId), "lastHeartbeatAt", 
                String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            // 忽略错误
        }
    }

    /**
     * 解除设备绑定（设备离线时调用）
     */
    public void unbind(String deviceId) {
        if (Strings.isBlank(deviceId)) {
            return;
        }
        bindingByDeviceId.remove(deviceId);
        String deviceCode = deviceCodeByDeviceId.remove(deviceId);
        if (Strings.isNotBlank(deviceCode)) {
            bindingByDeviceCode.remove(deviceCode);
        }
    }

    private String routeKey(String tenantId, String deviceId) {
        return "wk:device:gateway:route:" + tenantId + ":" + deviceId;
    }

    private String runtimeKey(String tenantId, String deviceId) {
        return "wk:device:runtime:" + tenantId + ":" + deviceId;
    }
}
