package com.budwk.sp.device.gateway.mq;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.gateway.service.GatewayDownlinkService;
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
        topic = "${wk.device.gateway.downlink-topic:wk.device.downlink.command}",
        consumerGroup = "${spring.application.name}-downlink",
        messageModel = MessageModel.BROADCASTING
)
public class RocketGatewayDownlinkConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final GatewayDownlinkService gatewayDownlinkService;

    public RocketGatewayDownlinkConsumer(ObjectMapper objectMapper, GatewayDownlinkService gatewayDownlinkService) {
        this.objectMapper = objectMapper;
        this.gatewayDownlinkService = gatewayDownlinkService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            gatewayDownlinkService.handle(envelope);
        } catch (Exception ignored) {
        }
    }
}
