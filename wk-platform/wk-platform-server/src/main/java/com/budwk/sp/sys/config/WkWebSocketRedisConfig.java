package com.budwk.sp.sys.config;

import com.budwk.sp.sys.websocket.WkWebSocketSessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket Redis 广播配置
 */
@Configuration
public class WkWebSocketRedisConfig {
    @Bean
    public RedisMessageListenerContainer wkWebSocketRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                                  MessageListener wkWebSocketMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(wkWebSocketMessageListener, new ChannelTopic(WkWebSocketSessionRegistry.WS_CHANNEL));
        return container;
    }

    @Bean
    public MessageListener wkWebSocketMessageListener(WkWebSocketSessionRegistry sessionRegistry) {
        return new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                sessionRegistry.handleDispatchMessage(new String(message.getBody(), StandardCharsets.UTF_8));
            }
        };
    }
}
