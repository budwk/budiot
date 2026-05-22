package com.budwk.sp.device.handler.config;

import com.budwk.sp.device.handler.service.DeviceProtocolScriptCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class DeviceHandlerRedisConfig {
    @Bean
    public RedisMessageListenerContainer deviceHandlerRedisListenerContainer(RedisConnectionFactory connectionFactory,
                                                                            MessageListener deviceHandlerScriptRefreshListener,
                                                                            DeviceHandlerProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(deviceHandlerScriptRefreshListener, new ChannelTopic(properties.getScriptRefreshChannel()));
        return container;
    }

    @Bean
    public MessageListener deviceHandlerScriptRefreshListener(DeviceProtocolScriptCacheService cacheService) {
        return new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                cacheService.handleRefreshMessage(new String(message.getBody(), StandardCharsets.UTF_8));
            }
        };
    }
}
