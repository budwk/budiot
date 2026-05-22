package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.gateway.service.GatewayControlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "KAFKA")
public class KafkaGatewayControlConsumer {
    private final ObjectMapper objectMapper;
    private final GatewayControlService gatewayControlService;

    public KafkaGatewayControlConsumer(ObjectMapper objectMapper, GatewayControlService gatewayControlService) {
        this.objectMapper = objectMapper;
        this.gatewayControlService = gatewayControlService;
    }

    @KafkaListener(topics = "#{@deviceGatewayProperties.broadcastTopic}", groupId = "#{@deviceGatewayProperties.nodeId + '-control'}")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayControlService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
