package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(topic = "${wk.device.database-ext.raw-archive-topic:wk.device.uplink.raw.archive}", consumerGroup = "${spring.application.name}-raw-archive", messageModel = MessageModel.CLUSTERING)
public class RocketRawArchiveDatabaseConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;

    public RocketRawArchiveDatabaseConsumer(ObjectMapper objectMapper, DeviceDatabaseDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            dispatchService.handleRaw(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
