package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.service.GatewayControlService;
import com.budwk.sp.device.gateway.service.GatewayDownlinkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "REDIS")
public class RedisGatewayMessageSubscriber {
    private final ObjectMapper objectMapper;
    private final GatewayDownlinkService gatewayDownlinkService;
    private final GatewayControlService gatewayControlService;

    public RedisGatewayMessageSubscriber(ObjectMapper objectMapper,
                                         GatewayDownlinkService gatewayDownlinkService,
                                         GatewayControlService gatewayControlService) {
        this.objectMapper = objectMapper;
        this.gatewayDownlinkService = gatewayDownlinkService;
        this.gatewayControlService = gatewayControlService;
    }

    public void onDownlink(Message message, byte[] pattern) {
        try {
            DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), new TypeReference<>() {});
            gatewayDownlinkService.handle(envelope);
        } catch (Exception ignored) {
        }
    }

    public void onControl(Message message, byte[] pattern) {
        try {
            DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), new TypeReference<>() {});
            gatewayControlService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
