package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceUplinkMessageDTO;
import com.budwk.sp.device.rule.service.DeviceRuleEngineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitRuleConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceRuleEngineService engineService;

    public RabbitRuleConsumer(ObjectMapper objectMapper, DeviceRuleEngineService engineService) {
        this.objectMapper = objectMapper;
        this.engineService = engineService;
    }

    @RabbitListener(queues = "#{@deviceRuleNormalizedQueue.name}")
    public void onNormalized(String message) {
        try {
            DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleNormalized(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "#{@deviceRuleEventQueue.name}")
    public void onEvent(String message) {
        try {
            DeviceMessageEnvelope<DeviceEventMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleEvent(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
