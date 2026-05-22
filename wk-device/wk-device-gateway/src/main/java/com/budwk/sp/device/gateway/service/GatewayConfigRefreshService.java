package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关配置刷新服务
 * 支持实时推送配置变更到所有网关节点
 */
@Slf4j
@Service
public class GatewayConfigRefreshService {
    
    private final DeviceMessageTemplate messageTemplate;
    private final GatewayNetworkManager networkManager;
    
    public GatewayConfigRefreshService(DeviceMessageTemplate messageTemplate,
                                       GatewayNetworkManager networkManager) {
        this.messageTemplate = messageTemplate;
        this.networkManager = networkManager;
    }
    
    /**
     * 通知所有节点刷新指定网关配置
     */
    public void notifyRefresh(String gatewayId) {
        if (Strings.isBlank(gatewayId)) {
            log.warn("Gateway ID is blank, skip refresh notification");
            return;
        }
        
        try {
            DeviceMessageEnvelope<Map<String, String>> envelope = new DeviceMessageEnvelope<>();
            envelope.setScene(DeviceMessageScene.GATEWAY_CONTROL);
            envelope.setPattern(DeviceMessagePattern.BROADCAST);
            envelope.setRoutingKey("gateway.refresh");
            
            Map<String, String> payload = new HashMap<>();
            payload.put("action", "REFRESH");
            payload.put("gatewayId", gatewayId);
            payload.put("timestamp", String.valueOf(System.currentTimeMillis()));
            envelope.setPayload(payload);
            
            messageTemplate.publish(envelope, gatewayId, "system");
            
            log.info("Sent gateway refresh notification: gatewayId={}", gatewayId);
            
        } catch (Exception e) {
            log.error("Failed to send gateway refresh notification: gatewayId={}", gatewayId, e);
        }
    }
    
    /**
     * 通知所有节点重新加载所有网关配置
     */
    public void notifyReloadAll() {
        try {
            DeviceMessageEnvelope<Map<String, String>> envelope = new DeviceMessageEnvelope<>();
            envelope.setScene(DeviceMessageScene.GATEWAY_CONTROL);
            envelope.setPattern(DeviceMessagePattern.BROADCAST);
            envelope.setRoutingKey("gateway.reload.all");
            
            Map<String, String> payload = new HashMap<>();
            payload.put("action", "RELOAD_ALL");
            payload.put("timestamp", String.valueOf(System.currentTimeMillis()));
            envelope.setPayload(payload);
            
            messageTemplate.publish(envelope, "all", "system");
            
            log.info("Sent gateway reload all notification");
            
        } catch (Exception e) {
            log.error("Failed to send gateway reload all notification", e);
        }
    }
    
    /**
     * 处理配置刷新消息（由消息消费者调用）
     */
    public void handleRefreshMessage(Map<String, String> payload) {
        String action = payload.get("action");
        String gatewayId = payload.get("gatewayId");
        
        if ("REFRESH".equals(action) && !Strings.isBlank(gatewayId)) {
            try {
                networkManager.refreshGateway(gatewayId);
                log.info("Refreshed gateway config: gatewayId={}", gatewayId);
            } catch (Exception e) {
                log.error("Failed to refresh gateway config: gatewayId={}", gatewayId, e);
            }
        } else if ("RELOAD_ALL".equals(action)) {
            try {
                networkManager.sync();
                log.info("Reloaded all gateway configs");
            } catch (Exception e) {
                log.error("Failed to reload all gateway configs", e);
            }
        } else {
            log.warn("Unknown refresh action: {}", action);
        }
    }
}
