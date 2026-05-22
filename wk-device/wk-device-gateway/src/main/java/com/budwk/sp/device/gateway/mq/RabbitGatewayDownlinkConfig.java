package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitGatewayDownlinkConfig {
    @Bean
    public TopicExchange deviceGatewayRabbitExchange(DeviceGatewayProperties properties) {
        return new TopicExchange(properties.getDownlinkTopic(), true, false);
    }

    @Bean
    public TopicExchange deviceGatewayControlExchange(DeviceGatewayProperties properties) {
        return new TopicExchange(properties.getBroadcastTopic(), true, false);
    }

    @Bean
    public Queue deviceGatewayRabbitQueue(DeviceGatewayProperties properties) {
        return new Queue("wk.device.gateway." + properties.getNodeId() + ".downlink", true);
    }

    @Bean
    public Binding deviceGatewayRabbitBinding(Queue deviceGatewayRabbitQueue, TopicExchange deviceGatewayRabbitExchange) {
        return BindingBuilder.bind(deviceGatewayRabbitQueue).to(deviceGatewayRabbitExchange).with("#");
    }

    @Bean
    public Queue deviceGatewayControlQueue(DeviceGatewayProperties properties) {
        return new Queue("wk.device.gateway." + properties.getNodeId() + ".control", true);
    }

    @Bean
    public Binding deviceGatewayControlBinding(Queue deviceGatewayControlQueue, TopicExchange deviceGatewayControlExchange) {
        return BindingBuilder.bind(deviceGatewayControlQueue).to(deviceGatewayControlExchange).with("#");
    }
}
