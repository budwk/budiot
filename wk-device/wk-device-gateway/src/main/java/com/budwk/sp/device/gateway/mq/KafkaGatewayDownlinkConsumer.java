package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.gateway.service.GatewayDownlinkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "KAFKA")
public class KafkaGatewayDownlinkConsumer {
    private final ObjectMapper objectMapper;
    private final GatewayDownlinkService gatewayDownlinkService;

    public KafkaGatewayDownlinkConsumer(ObjectMapper objectMapper, GatewayDownlinkService gatewayDownlinkService) {
        this.objectMapper = objectMapper;
        this.gatewayDownlinkService = gatewayDownlinkService;
    }

    @KafkaListener(topics = "#{@deviceGatewayProperties.downlinkTopic}", groupId = "#{@deviceGatewayProperties.nodeId + '-downlink'}")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayDownlinkService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
