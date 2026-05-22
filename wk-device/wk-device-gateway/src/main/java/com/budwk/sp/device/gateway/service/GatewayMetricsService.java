package com.budwk.sp.device.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关监控服务
 * 收集和暴露关键监控指标
 */
@Slf4j
@Service
public class GatewayMetricsService {
    
    private final StringRedisTemplate redisTemplate;
    private final GatewayNetworkManager networkManager;
    
    // 指标缓存
    private volatile Map<String, Object> cachedMetrics = new HashMap<>();
    private volatile long lastCollectTime = 0;
    
    public GatewayMetricsService(StringRedisTemplate redisTemplate, 
                                 GatewayNetworkManager networkManager) {
        this.redisTemplate = redisTemplate;
        this.networkManager = networkManager;
    }
    
    /**
     * 定期收集监控指标（每分钟）
     */
    @Scheduled(fixedDelay = 60000L)
    public void collectMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // 1. 网关运行状态
            metrics.put("gateway.total", networkManager.getServerMap().size());
            metrics.put("gateway.running", networkManager.getServerMap().values().stream()
                .filter(server -> server.isRunning()).count());
            metrics.put("gateway.stopped", networkManager.getServerMap().values().stream()
                .filter(server -> !server.isRunning()).count());
            
            // 2. Redis 连接状态
            try {
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                metrics.put("redis.connected", "PONG".equalsIgnoreCase(pong));
            } catch (Exception e) {
                metrics.put("redis.connected", false);
            }
            
            // 3. 消息队列堆积（示例：检查 stream 长度）
            // TODO: 实际实现需要遍历所有 stream
            metrics.put("queue.backlog", 0);
            
            // 4. 系统资源
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            metrics.put("jvm.memory.used", usedMemory);
            metrics.put("jvm.memory.total", runtime.totalMemory());
            metrics.put("jvm.memory.max", runtime.maxMemory());
            metrics.put("jvm.memory.usage", (double) usedMemory / runtime.maxMemory() * 100);
            
            // 5. 时间戳
            metrics.put("collect.time", System.currentTimeMillis());
            
            // 更新缓存
            cachedMetrics = metrics;
            lastCollectTime = System.currentTimeMillis();
            
            log.debug("Collected metrics: {}", metrics);
            
            // TODO: 上报到监控系统
            // Prometheus、InfluxDB、OpenTelemetry 等
            // reportToPrometheus(metrics);
            
        } catch (Exception e) {
            log.error("Failed to collect metrics", e);
        }
    }
    
    /**
     * 获取缓存的监控指标
     */
    public Map<String, Object> getMetrics() {
        return new HashMap<>(cachedMetrics);
    }
    
    /**
     * 获取上次收集时间
     */
    public long getLastCollectTime() {
        return lastCollectTime;
    }
    
    /**
     * 检查是否健康（最近 5 分钟内收集过指标）
     */
    public boolean isHealthy() {
        return (System.currentTimeMillis() - lastCollectTime) < 300000L;
    }
    
    /**
     * 上报到 Prometheus（示例）
     */
    private void reportToPrometheus(Map<String, Object> metrics) {
        // TODO: 实现 Prometheus 指标上报
        // 可以使用 micrometer-registry-prometheus
        /*
        metrics.forEach((key, value) -> {
            if (value instanceof Number) {
                Gauge.builder("wk_device_" + key.replace('.', '_'), () -> (Number) value)
                    .register(registry);
            }
        });
        */
    }
}
