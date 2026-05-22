package com.budwk.sp.device.handler.config;

import com.budwk.sp.device.handler.service.DeviceProtocolScriptCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 协议脚本缓存刷新监听器
 * 
 * 监听 Redis Pub/Sub 消息，刷新本地缓存
 */
@Slf4j
@Component
public class ProtocolCacheRefreshListener implements MessageListener {
    private final DeviceProtocolScriptCacheService cacheService;

    public ProtocolCacheRefreshListener(DeviceProtocolScriptCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            String channel = new String(message.getChannel());
            
            log.debug("收到协议缓存刷新消息: channel={}", channel);
            
            cacheService.handleRefreshMessage(body);
            
        } catch (Exception e) {
            log.error("处理协议缓存刷新消息失败: {}", e.getMessage(), e);
        }
    }
}
