package com.budwk.sp.device.handler.mq;

import com.budwk.sp.device.handler.config.DeviceHandlerProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitRawUplinkConfig {
    @Bean
    public TopicExchange deviceHandlerRawExchange(DeviceHandlerProperties properties) {
        return new TopicExchange(properties.getRawTopic(), true, false);
    }

    @Bean
    public Queue deviceHandlerRawQueue() {
        return new Queue("wk.device.handler.raw", true);
    }

    @Bean
    public Binding deviceHandlerRawBinding(Queue deviceHandlerRawQueue, TopicExchange deviceHandlerRawExchange) {
        return BindingBuilder.bind(deviceHandlerRawQueue).to(deviceHandlerRawExchange).with("#");
    }
}
