package com.budwk.sp.device.message.sender;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageProviderType;
import com.budwk.sp.starter.common.exception.BaseException;
import org.nutz.lang.Strings;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class RabbitDeviceMessageSender implements DeviceMessageSender {
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public RabbitDeviceMessageSender(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    @Override
    public DeviceMessageProviderType provider() {
        return DeviceMessageProviderType.RABBITMQ;
    }

    @Override
    public void send(DeviceMessageEnvelope<?> envelope, String payloadJson) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) throw new BaseException("RabbitMQTemplate 未配置");
        rabbitTemplate.convertAndSend(
                envelope.getTopic(),
                envelope.getPattern() == DeviceMessagePattern.BROADCAST ? "" : Strings.sBlank(envelope.getRoutingKey(), envelope.getTopic()),
                payloadJson,
                (Message message) -> {
                    if (envelope.getDelaySeconds() != null && envelope.getDelaySeconds() > 0) {
                        message.getMessageProperties().setDelayLong(envelope.getDelaySeconds() * 1000L);
                    }
                    message.getMessageProperties().setHeader("x-device-scene", envelope.getScene().getValue());
                    message.getMessageProperties().setHeader("x-device-pattern", envelope.getPattern().getValue());
                    message.getMessageProperties().setHeader("x-device-message-id", envelope.getMessageId());
                    return message;
                }
        );
    }
}
