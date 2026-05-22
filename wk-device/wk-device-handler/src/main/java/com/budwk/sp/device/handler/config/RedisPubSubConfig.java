package com.budwk.sp.device.handler.config;

import com.budwk.sp.device.handler.service.DeviceProtocolScriptCacheService;
import com.budwk.sp.device.support.DeviceScriptChannels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis Pub/Sub 配置
 * 
 * 用于协议脚本缓存更新的广播通知
 */
@Slf4j
@Configuration
public class RedisPubSubConfig {

    /**
     * Redis 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ProtocolCacheRefreshListener protocolCacheRefreshListener) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 添加协议缓存刷新监听器
        container.addMessageListener(
            protocolCacheRefreshListener,
            new PatternTopic(DeviceScriptChannels.SCRIPT_REFRESH)
        );
        
        log.info("Redis Pub/Sub 监听容器已配置，监听频道: {}", DeviceScriptChannels.SCRIPT_REFRESH);
        
        return container;
    }

    /**
     * 协议缓存刷新监听器
     */
    @Bean
    public ProtocolCacheRefreshListener protocolCacheRefreshListener(
            DeviceProtocolScriptCacheService cacheService) {
        return new ProtocolCacheRefreshListener(cacheService);
    }
}
