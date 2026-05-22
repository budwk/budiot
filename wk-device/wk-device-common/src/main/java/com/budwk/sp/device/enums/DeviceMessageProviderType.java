package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessageProviderType {
    REDIS("REDIS", "Redis"), RABBITMQ("RABBITMQ", "RabbitMQ"), KAFKA("KAFKA", "Kafka"), ROCKETMQ("ROCKETMQ", "RocketMQ");
    private final String value; private final String text;
    DeviceMessageProviderType(String value, String text) { this.value = value; this.text = text; }
}
