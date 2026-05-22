package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.dto.DeviceScriptRefreshMessageDTO;
import com.budwk.sp.device.entity.Device_protocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备协议脚本缓存服务
 * 
 * 改进：
 * 1. 使用 Redis Pub/Sub 广播缓存更新
 * 2. 所有实例监听并刷新本地缓存
 * 3. 支持配置缓存过期时间
 */
@Slf4j
@Service
public class DeviceProtocolScriptCacheService {
    private final ObjectMapper objectMapper;
    private final DeviceEntityRedisCacheService deviceEntityRedisCacheService;
    private final Map<String, Device_protocol> scriptCache = new ConcurrentHashMap<>();
    
    private static final long CACHE_EXPIRE_SECONDS = 1800; // 30分钟

    public DeviceProtocolScriptCacheService(ObjectMapper objectMapper, 
                                             DeviceEntityRedisCacheService deviceEntityRedisCacheService) {
        this.objectMapper = objectMapper;
        this.deviceEntityRedisCacheService = deviceEntityRedisCacheService;
    }

    @PostConstruct
    public void preload() {
        log.info("协议脚本缓存服务已启动");
    }

    public Device_protocol getProtocol(String tenantId, String protocolId) {
        if (Strings.isBlank(tenantId) || Strings.isBlank(protocolId)) {
            return null;
        }
        
        String cacheKey = cacheKey(tenantId, protocolId);
        
        // 1. 先从本地缓存获取
        Device_protocol cached = scriptCache.get(cacheKey);
        if (cached != null) {
            // 检查缓存是否过期
            if (isCacheExpired(cached)) {
                scriptCache.remove(cacheKey);
                cached = null;
            }
        }
        
        // 2. 如果本地缓存不存在，从 Redis 加载
        if (cached == null) {
            cached = scriptCache.computeIfAbsent(cacheKey, key -> 
                deviceEntityRedisCacheService.getProtocol(tenantId, protocolId)
            );
        }
        
        return cached;
    }

    /**
     * 处理 Redis Pub/Sub 刷新消息
     */
    public void handleRefreshMessage(String body) {
        if (Strings.isBlank(body)) {
            return;
        }
        try {
            DeviceScriptRefreshMessageDTO message = objectMapper.readValue(body, DeviceScriptRefreshMessageDTO.class);
            if (Strings.isBlank(message.getTenantId()) || Strings.isBlank(message.getProtocolId())) {
                return;
            }
            
            String cacheKey = cacheKey(message.getTenantId(), message.getProtocolId());
            
            if ("DELETE".equalsIgnoreCase(message.getAction())) {
                scriptCache.remove(cacheKey);
                log.info("协议脚本缓存已删除: tenantId={}, protocolId={}", 
                    message.getTenantId(), message.getProtocolId());
                return;
            }
            
            Device_protocol protocol = deviceEntityRedisCacheService.getProtocol(
                message.getTenantId(), message.getProtocolId()
            );
            
            if (protocol == null) {
                scriptCache.remove(cacheKey);
                log.warn("协议脚本不存在，已从缓存删除: tenantId={}, protocolId={}", 
                    message.getTenantId(), message.getProtocolId());
                return;
            }
            
            scriptCache.put(cacheKey, protocol);
            log.info("协议脚本缓存已更新: tenantId={}, protocolId={}, action={}", 
                message.getTenantId(), message.getProtocolId(), message.getAction());
                
        } catch (Exception e) {
            log.warn("处理协议脚本刷新消息失败: {}", e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        int size = scriptCache.size();
        scriptCache.clear();
        log.info("已清空所有协议脚本缓存: count={}", size);
    }

    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired(Device_protocol protocol) {
        if (protocol.getUpdatedAt() == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long cacheAge = (now - protocol.getUpdatedAt()) / 1000;
        
        return cacheAge > CACHE_EXPIRE_SECONDS;
    }

    private String cacheKey(String tenantId, String protocolId) {
        return tenantId + ":" + protocolId;
    }
    
    @PreDestroy
    public void destroy() {
        log.info("协议脚本缓存服务已停止，清空缓存: count={}", scriptCache.size());
        scriptCache.clear();
    }
}
