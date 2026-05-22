package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.support.DeviceRedisMessageSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "REDIS")
public class RedisGatewayMessageConfig {
    @Bean
    public RedisMessageListenerContainer deviceGatewayRedisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
                                                                                    RedisGatewayMessageSubscriber subscriber,
                                                                                    DeviceGatewayProperties gatewayProperties,
                                                                                    DeviceMessageProperties messageProperties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber::onDownlink, new PatternTopic(DeviceRedisMessageSupport.channelPattern(messageProperties, gatewayProperties.getDownlinkTopic())));
        container.addMessageListener(subscriber::onControl, new PatternTopic(DeviceRedisMessageSupport.channelPattern(messageProperties, gatewayProperties.getBroadcastTopic())));
        return container;
    }
}
