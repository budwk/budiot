package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitDatabaseConfig {
    @Bean public TopicExchange deviceDatabaseRawExchange(DeviceDatabaseProperties p) { return new TopicExchange(p.getRawTopic(), true, false); }
    @Bean public TopicExchange deviceDatabaseRawArchiveExchange(DeviceDatabaseProperties p) { return new TopicExchange(p.getRawArchiveTopic(), true, false); }
    @Bean public TopicExchange deviceDatabaseNormalizedExchange(DeviceDatabaseProperties p) { return new TopicExchange(p.getNormalizedTopic(), true, false); }
    @Bean public TopicExchange deviceDatabaseEventExchange(DeviceDatabaseProperties p) { return new TopicExchange(p.getEventTopic(), true, false); }
    @Bean public TopicExchange deviceDatabaseDownlinkExchange(DeviceDatabaseProperties p) { return new TopicExchange(p.getDownlinkTopic(), true, false); }
    @Bean public Queue deviceDatabaseRawQueue() { return new Queue("wk.device.database.raw", true); }
    @Bean public Queue deviceDatabaseRawArchiveQueue() { return new Queue("wk.device.database.raw.archive", true); }
    @Bean public Queue deviceDatabaseNormalizedQueue() { return new Queue("wk.device.database.normalized", true); }
    @Bean public Queue deviceDatabaseEventQueue() { return new Queue("wk.device.database.event", true); }
    @Bean public Queue deviceDatabaseDownlinkQueue() { return new Queue("wk.device.database.downlink", true); }
    @Bean public Binding deviceDatabaseRawBinding(Queue deviceDatabaseRawQueue, TopicExchange deviceDatabaseRawExchange) { return BindingBuilder.bind(deviceDatabaseRawQueue).to(deviceDatabaseRawExchange).with("#"); }
    @Bean public Binding deviceDatabaseRawArchiveBinding(Queue deviceDatabaseRawArchiveQueue, TopicExchange deviceDatabaseRawArchiveExchange) { return BindingBuilder.bind(deviceDatabaseRawArchiveQueue).to(deviceDatabaseRawArchiveExchange).with("#"); }
    @Bean public Binding deviceDatabaseNormalizedBinding(Queue deviceDatabaseNormalizedQueue, TopicExchange deviceDatabaseNormalizedExchange) { return BindingBuilder.bind(deviceDatabaseNormalizedQueue).to(deviceDatabaseNormalizedExchange).with("#"); }
    @Bean public Binding deviceDatabaseEventBinding(Queue deviceDatabaseEventQueue, TopicExchange deviceDatabaseEventExchange) { return BindingBuilder.bind(deviceDatabaseEventQueue).to(deviceDatabaseEventExchange).with("#"); }
    @Bean public Binding deviceDatabaseDownlinkBinding(Queue deviceDatabaseDownlinkQueue, TopicExchange deviceDatabaseDownlinkExchange) { return BindingBuilder.bind(deviceDatabaseDownlinkQueue).to(deviceDatabaseDownlinkExchange).with("#"); }
}
