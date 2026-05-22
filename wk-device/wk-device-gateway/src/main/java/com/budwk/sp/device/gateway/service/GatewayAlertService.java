package com.budwk.sp.device.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关告警服务
 * 负责发送网关相关的告警通知
 */
@Slf4j
@Service
public class GatewayAlertService {
    
    // 告警冷却时间（毫秒），避免同一告警频繁发送
    private static final long ALERT_COOLDOWN_MS = 300000L; // 5 分钟
    
    // 记录最近一次告警时间
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    
    /**
     * 发送网关启动失败告警
     */
    public void alertStartFailed(String gatewayId, String gatewayName, String errorMessage) {
        String alertKey = "start_failed:" + gatewayId;
        if (!shouldAlert(alertKey)) {
            return;
        }
        
        String message = String.format("[网关启动失败] 网关ID: %s, 名称: %s, 错误: %s", 
            gatewayId, 
            Strings.sBlank(gatewayName, "未知"), 
            Strings.sBlank(errorMessage, "未知错误")
        );
        
        sendAlert("GATEWAY_START_FAILED", message, Map.of(
            "gatewayId", gatewayId,
            "gatewayName", Strings.sBlank(gatewayName, "未知"),
            "error", Strings.sBlank(errorMessage, "未知错误")
        ));
    }
    
    /**
     * 发送网关异常停止告警
     */
    public void alertAbnormalStop(String gatewayId, String gatewayName) {
        String alertKey = "abnormal_stop:" + gatewayId;
        if (!shouldAlert(alertKey)) {
            return;
        }
        
        String message = String.format("[网关异常停止] 网关ID: %s, 名称: %s", 
            gatewayId, 
            Strings.sBlank(gatewayName, "未知")
        );
        
        sendAlert("GATEWAY_ABNORMAL_STOP", message, Map.of(
            "gatewayId", gatewayId,
            "gatewayName", Strings.sBlank(gatewayName, "未知")
        ));
    }
    
    /**
     * 发送网关重启失败告警
     */
    public void alertRestartFailed(String gatewayId, String gatewayName, String errorMessage) {
        String alertKey = "restart_failed:" + gatewayId;
        if (!shouldAlert(alertKey)) {
            return;
        }
        
        String message = String.format("[网关重启失败] 网关ID: %s, 名称: %s, 错误: %s", 
            gatewayId, 
            Strings.sBlank(gatewayName, "未知"), 
            Strings.sBlank(errorMessage, "未知错误")
        );
        
        sendAlert("GATEWAY_RESTART_FAILED", message, Map.of(
            "gatewayId", gatewayId,
            "gatewayName", Strings.sBlank(gatewayName, "未知"),
            "error", Strings.sBlank(errorMessage, "未知错误")
        ));
    }
    
    /**
     * 发送端口冲突告警
     */
    public void alertPortConflict(String gatewayId, String host, int port) {
        String alertKey = "port_conflict:" + gatewayId;
        if (!shouldAlert(alertKey)) {
            return;
        }
        
        String message = String.format("[端口冲突] 网关ID: %s, 地址: %s:%d", 
            gatewayId, host, port
        );
        
        sendAlert("GATEWAY_PORT_CONFLICT", message, Map.of(
            "gatewayId", gatewayId,
            "host", host,
            "port", String.valueOf(port)
        ));
    }
    
    /**
     * 发送消息队列堆积告警
     */
    public void alertQueueBacklog(String queueName, long backlogCount) {
        String alertKey = "queue_backlog:" + queueName;
        if (!shouldAlert(alertKey)) {
            return;
        }
        
        String message = String.format("[消息队列堆积] 队列: %s, 堆积数量: %d", 
            queueName, backlogCount
        );
        
        sendAlert("QUEUE_BACKLOG", message, Map.of(
            "queueName", queueName,
            "backlogCount", String.valueOf(backlogCount)
        ));
    }
    
    /**
     * 判断是否应该发送告警（基于冷却时间）
     */
    private boolean shouldAlert(String alertKey) {
        long now = System.currentTimeMillis();
        Long lastTime = lastAlertTime.get(alertKey);
        
        if (lastTime == null || (now - lastTime) > ALERT_COOLDOWN_MS) {
            lastAlertTime.put(alertKey, now);
            return true;
        }
        
        return false;
    }
    
    /**
     * 实际发送告警（可扩展为邮件、短信、钉钉、企业微信等）
     */
    private void sendAlert(String alertType, String message, Map<String, String> context) {
        // 当前实现：仅记录日志
        log.warn("[ALERT] type={}, message={}, context={}", alertType, message, context);
        
        // TODO: 扩展点
        // 1. 发送邮件通知
        // 2. 发送短信通知
        // 3. 发送钉钉/企业微信机器人消息
        // 4. 调用第三方告警平台 API
        // 5. 写入告警历史表
        
        /*
        示例：钉钉机器人通知
        DingTalkNotifier.send(message, context);
        
        示例：邮件通知
        EmailNotifier.send(alertType, message, context);
        
        示例：写入数据库
        alertHistoryService.record(alertType, message, context);
        */
    }
    
    /**
     * 清理过期的告警记录（定时任务调用）
     */
    public void cleanupExpiredAlerts() {
        long expireTime = System.currentTimeMillis() - ALERT_COOLDOWN_MS * 2;
        lastAlertTime.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }
}
