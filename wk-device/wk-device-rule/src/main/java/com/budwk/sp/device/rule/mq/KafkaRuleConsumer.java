package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceUplinkMessageDTO;
import com.budwk.sp.device.rule.service.DeviceRuleEngineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "KAFKA")
public class KafkaRuleConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceRuleEngineService engineService;

    public KafkaRuleConsumer(ObjectMapper objectMapper, DeviceRuleEngineService engineService) {
        this.objectMapper = objectMapper;
        this.engineService = engineService;
    }

    @KafkaListener(topics = "${wk.device.rule.normalized-topic:wk.device.uplink.normalized}", groupId = "${spring.application.name}-normalized")
    public void onNormalized(String message) {
        try {
            DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleNormalized(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${wk.device.rule.event-topic:wk.device.event}", groupId = "${spring.application.name}-event")
    public void onEvent(String message) {
        try {
            DeviceMessageEnvelope<DeviceEventMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleEvent(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
