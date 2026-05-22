package com.budwk.sp.device.message.sender;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageProviderType;
import com.budwk.sp.starter.common.exception.BaseException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RocketDeviceMessageSender implements DeviceMessageSender {
    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    public RocketDeviceMessageSender(ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
    }

    @Override
    public DeviceMessageProviderType provider() {
        return DeviceMessageProviderType.ROCKETMQ;
    }

    @Override
    public void send(DeviceMessageEnvelope<?> envelope, String payloadJson) {
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) throw new BaseException("RocketMQTemplate 未配置");
        Message<String> message = MessageBuilder.withPayload(payloadJson)
                .setHeader("KEYS", envelope.getMessageId())
                .setHeader("scene", envelope.getScene().getValue())
                .setHeader("pattern", envelope.getPattern().getValue())
                .setHeader("routingKey", Strings.sBlank(envelope.getRoutingKey(), envelope.getDeviceCode()))
                .build();
        String destination = envelope.getPattern() == DeviceMessagePattern.BROADCAST || Strings.isBlank(envelope.getRoutingKey())
                ? envelope.getTopic()
                : envelope.getTopic() + ":" + envelope.getRoutingKey();
        if (envelope.getDelaySeconds() != null && envelope.getDelaySeconds() > 0) {
            rocketMQTemplate.syncSend(destination, message, 3000L, resolveDelayLevel(envelope.getDelaySeconds()));
            return;
        }
        rocketMQTemplate.syncSend(destination, message, 3000L);
    }

    private int resolveDelayLevel(int delaySeconds) {
        if (delaySeconds <= 1) return 1;
        if (delaySeconds <= 5) return 2;
        if (delaySeconds <= 10) return 3;
        if (delaySeconds <= 30) return 4;
        if (delaySeconds <= 60) return 5;
        if (delaySeconds <= 120) return 6;
        if (delaySeconds <= 180) return 7;
        if (delaySeconds <= 240) return 8;
        if (delaySeconds <= 300) return 9;
        return 10;
    }
}
