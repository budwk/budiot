package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(topic = "${wk.device.database-ext.event-topic:wk.device.event}", consumerGroup = "${spring.application.name}-event", messageModel = MessageModel.CLUSTERING)
public class RocketEventDatabaseConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;

    public RocketEventDatabaseConsumer(ObjectMapper objectMapper, DeviceDatabaseDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceEventMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            dispatchService.handleEvent(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
