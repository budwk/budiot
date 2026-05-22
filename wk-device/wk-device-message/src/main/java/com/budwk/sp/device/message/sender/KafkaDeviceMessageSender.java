package com.budwk.sp.device.message.sender;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageProviderType;
import com.budwk.sp.starter.common.exception.BaseException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class KafkaDeviceMessageSender implements DeviceMessageSender {
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    public KafkaDeviceMessageSender(ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    @Override
    public DeviceMessageProviderType provider() {
        return DeviceMessageProviderType.KAFKA;
    }

    @Override
    public void send(DeviceMessageEnvelope<?> envelope, String payloadJson) {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) throw new BaseException("KafkaTemplate 未配置");
        String key = envelope.getPattern() == DeviceMessagePattern.BROADCAST ? null : Strings.sBlank(envelope.getRoutingKey(), envelope.getDeviceCode());
        ProducerRecord<String, String> record = new ProducerRecord<>(envelope.getTopic(), key, payloadJson);
        record.headers().add(new RecordHeader("x-device-scene", envelope.getScene().getValue().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("x-device-pattern", envelope.getPattern().getValue().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("x-device-message-id", envelope.getMessageId().getBytes(StandardCharsets.UTF_8)));
        if (envelope.getDelaySeconds() != null && envelope.getDelaySeconds() > 0) {
            record.headers().add(new RecordHeader("x-device-delay-seconds", String.valueOf(envelope.getDelaySeconds()).getBytes(StandardCharsets.UTF_8)));
        }
        kafkaTemplate.send(record);
    }
}
