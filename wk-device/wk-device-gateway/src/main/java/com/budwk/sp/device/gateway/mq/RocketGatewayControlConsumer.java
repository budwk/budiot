package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceGatewayControlMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.gateway.service.GatewayControlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(
        topic = "${wk.device.gateway.broadcast-topic:wk.device.gateway.broadcast}",
        consumerGroup = "${spring.application.name}-control",
        messageModel = MessageModel.BROADCASTING
)
public class RocketGatewayControlConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final GatewayControlService gatewayControlService;

    public RocketGatewayControlConsumer(ObjectMapper objectMapper, GatewayControlService gatewayControlService) {
        this.objectMapper = objectMapper;
        this.gatewayControlService = gatewayControlService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceGatewayControlMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayControlService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
