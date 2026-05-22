package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.rule.config.DeviceRuleProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitRuleConfig {
    @Bean
    public TopicExchange deviceRuleNormalizedExchange(DeviceRuleProperties properties) {
        return new TopicExchange(properties.getNormalizedTopic(), true, false);
    }

    @Bean
    public TopicExchange deviceRuleEventExchange(DeviceRuleProperties properties) {
        return new TopicExchange(properties.getEventTopic(), true, false);
    }

    @Bean
    public Queue deviceRuleNormalizedQueue() {
        return new Queue("wk.device.rule.normalized", true);
    }

    @Bean
    public Queue deviceRuleEventQueue() {
        return new Queue("wk.device.rule.event", true);
    }

    @Bean
    public Binding deviceRuleNormalizedBinding(Queue deviceRuleNormalizedQueue, TopicExchange deviceRuleNormalizedExchange) {
        return BindingBuilder.bind(deviceRuleNormalizedQueue).to(deviceRuleNormalizedExchange).with("#");
    }

    @Bean
    public Binding deviceRuleEventBinding(Queue deviceRuleEventQueue, TopicExchange deviceRuleEventExchange) {
        return BindingBuilder.bind(deviceRuleEventQueue).to(deviceRuleEventExchange).with("#");
    }
}
