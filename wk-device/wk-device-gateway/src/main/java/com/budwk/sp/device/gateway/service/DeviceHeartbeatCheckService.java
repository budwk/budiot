package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceEventLevel;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 设备心跳检测服务
 * 
 * 功能：
 * 1. 定时检测心跳超时的设备
 * 2. 更新设备在线状态
 * 3. 发送设备离线通知
 */
@Slf4j
@Service
public class DeviceHeartbeatCheckService {
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceMessageTemplate deviceMessageTemplate;
    private final GatewaySessionService gatewaySessionService;
    private final ObjectMapper objectMapper;

    @Value("${wk.device.gateway.heartbeat-timeout-seconds:60}")
    private int heartbeatTimeoutSeconds = 60;

    public DeviceHeartbeatCheckService(StringRedisTemplate stringRedisTemplate,
                                       DeviceMessageTemplate deviceMessageTemplate,
                                       GatewaySessionService gatewaySessionService,
                                       ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.deviceMessageTemplate = deviceMessageTemplate;
        this.gatewaySessionService = gatewaySessionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 定时检测心跳超时设备（每 30 秒执行一次）
     */
    @Scheduled(fixedDelayString = "${wk.device.gateway.heartbeat-check-seconds:30}000")
    public void checkHeartbeat() {
        long now = System.currentTimeMillis();
        long timeoutThreshold = now - (heartbeatTimeoutSeconds * 1000L);
        
        // 扫描所有运行时键
        Set<String> keys = stringRedisTemplate.keys("wk:device:runtime:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        int offlineCount = 0;
        for (String key : keys) {
            try {
                Map<Object, Object> runtime = stringRedisTemplate.opsForHash().entries(key);
                if (runtime.isEmpty()) {
                    continue;
                }
                
                String online = (String) runtime.get("online");
                if (!"true".equals(online)) {
                    continue;
                }
                
                String lastHeartbeatStr = (String) runtime.get("lastHeartbeatAt");
                if (Strings.isBlank(lastHeartbeatStr)) {
                    continue;
                }
                
                long lastHeartbeat = Long.parseLong(lastHeartbeatStr);
                if (lastHeartbeat < timeoutThreshold) {
                    // 设备心跳超时，标记为离线
                    markDeviceOffline(key, runtime, now);
                    offlineCount++;
                }
            } catch (Exception e) {
                log.error("检测设备心跳失败: key={}, error={}", key, e.getMessage());
            }
        }
        
        if (offlineCount > 0) {
            log.info("设备心跳检测完成: 检测到 {} 台设备离线", offlineCount);
        }
    }

    /**
     * 标记设备为离线
     */
    private void markDeviceOffline(String runtimeKey, Map<Object, Object> runtime, long now) {
        try {
            // 从 key 中提取 tenantId 和 deviceId
            String[] parts = runtimeKey.split(":");
            if (parts.length < 5) {
                return;
            }
            
            String tenantId = parts[3];
            String deviceId = parts[4];
            
            // 更新 Redis 运行时状态
            stringRedisTemplate.opsForHash().put(runtimeKey, "online", "false");
            stringRedisTemplate.opsForHash().put(runtimeKey, "offlineAt", String.valueOf(now));
            stringRedisTemplate.opsForHash().put(runtimeKey, "offlineReason", "heartbeat_timeout");
            
            // 清除会话绑定
            gatewaySessionService.unbind(deviceId);
            
            // 发送设备离线通知
            publishOfflineEvent(tenantId, deviceId, runtime);
            
            log.warn("设备心跳超时已标记为离线: tenantId={}, deviceId={}, lastHeartbeat={}", 
                tenantId, deviceId, runtime.get("lastHeartbeatAt"));
                
        } catch (Exception e) {
            log.error("标记设备离线失败: key={}, error={}", runtimeKey, e.getMessage());
        }
    }

    /**
     * 发送设备离线事件
     */
    private void publishOfflineEvent(String tenantId, String deviceId, Map<Object, Object> runtime) {
        try {
            long now = System.currentTimeMillis();
            Map<String, Object> content = new java.util.LinkedHashMap<>();
            content.put("deviceId", deviceId);
            content.put("tenantId", tenantId);
            content.put("offlineAt", now);
            content.put("reason", "heartbeat_timeout");
            content.put("lastHeartbeatAt", runtime.get("lastHeartbeatAt"));
            content.put("lastDeviceAt", runtime.get("lastDeviceAt"));
            content.put("ip", runtime.get("ip"));

            DeviceEventMessageDTO payload = new DeviceEventMessageDTO();
            payload.setEventCode("device_offline");
            payload.setEventName("设备离线");
            payload.setLevel(DeviceEventLevel.WARNING.getValue());
            payload.setSourceType(DeviceEventSourceType.DEVICE.getValue());
            payload.setContentJson(objectMapper.writeValueAsString(content));
            payload.setDeviceAt(now);

            DeviceMessageEnvelope<DeviceEventMessageDTO> envelope = new DeviceMessageEnvelope<>();
            envelope.setTenantId(tenantId);
            envelope.setProductId(String.valueOf(runtime.getOrDefault("productId", "")));
            envelope.setProductKey(String.valueOf(runtime.getOrDefault("productKey", "")));
            envelope.setDeviceId(deviceId);
            envelope.setDeviceCode(String.valueOf(runtime.getOrDefault("deviceCode", "")));
            envelope.setScene(DeviceMessageScene.EVENT);
            envelope.setPattern(DeviceMessagePattern.TOPIC);
            envelope.setOccurredAt(now);
            envelope.setRoutingKey(deviceId);
            envelope.getHeaders().put("eventType", "device_offline");
            envelope.setPayload(payload);
            
            deviceMessageTemplate.publish(envelope, deviceId, "heartbeat-checker");
            
        } catch (Exception e) {
            log.error("发送设备离线事件失败: deviceId={}, error={}", deviceId, e.getMessage());
        }
    }
}
