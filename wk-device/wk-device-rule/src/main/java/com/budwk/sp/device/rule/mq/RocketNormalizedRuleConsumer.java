package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceUplinkMessageDTO;
import com.budwk.sp.device.rule.service.DeviceRuleEngineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(topic = "${wk.device.rule.normalized-topic:wk.device.uplink.normalized}", consumerGroup = "${spring.application.name}-normalized", messageModel = MessageModel.CLUSTERING)
public class RocketNormalizedRuleConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceRuleEngineService engineService;

    public RocketNormalizedRuleConsumer(ObjectMapper objectMapper, DeviceRuleEngineService engineService) {
        this.objectMapper = objectMapper;
        this.engineService = engineService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleNormalized(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
