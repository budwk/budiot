package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.service.GatewayControlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitGatewayControlConsumer {
    private final ObjectMapper objectMapper;
    private final GatewayControlService gatewayControlService;

    public RabbitGatewayControlConsumer(ObjectMapper objectMapper, GatewayControlService gatewayControlService) {
        this.objectMapper = objectMapper;
        this.gatewayControlService = gatewayControlService;
    }

    @RabbitListener(queues = "#{@deviceGatewayControlQueue.name}")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayControlService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
