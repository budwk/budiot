package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
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
@RocketMQMessageListener(topic = "${wk.device.rule.event-topic:wk.device.event}", consumerGroup = "${spring.application.name}-event", messageModel = MessageModel.CLUSTERING)
public class RocketEventRuleConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceRuleEngineService engineService;

    public RocketEventRuleConsumer(ObjectMapper objectMapper, DeviceRuleEngineService engineService) {
        this.objectMapper = objectMapper;
        this.engineService = engineService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceEventMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            engineService.handleEvent(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
