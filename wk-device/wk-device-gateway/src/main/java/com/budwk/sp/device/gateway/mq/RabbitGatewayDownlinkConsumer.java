package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.service.GatewayDownlinkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitGatewayDownlinkConsumer {
    private final ObjectMapper objectMapper;
    private final GatewayDownlinkService gatewayDownlinkService;

    public RabbitGatewayDownlinkConsumer(ObjectMapper objectMapper, GatewayDownlinkService gatewayDownlinkService) {
        this.objectMapper = objectMapper;
        this.gatewayDownlinkService = gatewayDownlinkService;
    }

    @RabbitListener(queues = "#{@deviceGatewayRabbitQueue.name}")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayDownlinkService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
