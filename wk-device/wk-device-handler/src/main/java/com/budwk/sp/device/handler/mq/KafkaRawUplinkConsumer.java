package com.budwk.sp.device.handler.mq;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.budwk.sp.device.handler.service.DeviceRawMessageHandlerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "KAFKA")
public class KafkaRawUplinkConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceRawMessageHandlerService handlerService;

    public KafkaRawUplinkConsumer(ObjectMapper objectMapper, DeviceRawMessageHandlerService handlerService) {
        this.objectMapper = objectMapper;
        this.handlerService = handlerService;
    }

    @KafkaListener(topics = "${wk.device.handler.raw-topic:wk.device.uplink.raw}", groupId = "${spring.application.name}-raw-uplink")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            handlerService.handle(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
