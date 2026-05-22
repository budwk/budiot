package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitDatabaseConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;

    public RabbitDatabaseConsumer(ObjectMapper objectMapper, DeviceDatabaseDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = "#{@deviceDatabaseRawQueue.name}")
    public void onRaw(String message) throws Exception {
        dispatchService.handleRaw(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}));
    }

    @RabbitListener(queues = "#{@deviceDatabaseRawArchiveQueue.name}")
    public void onRawArchive(String message) throws Exception {
        dispatchService.handleRaw(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}));
    }

    @RabbitListener(queues = "#{@deviceDatabaseNormalizedQueue.name}")
    public void onNormalized(String message) throws Exception {
        dispatchService.handleNormalized(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceUplinkMessageDTO>>() {}));
    }

    @RabbitListener(queues = "#{@deviceDatabaseEventQueue.name}")
    public void onEvent(String message) throws Exception {
        dispatchService.handleEvent(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceEventMessageDTO>>() {}));
    }

    @RabbitListener(queues = "#{@deviceDatabaseDownlinkQueue.name}")
    public void onDownlink(String message) throws Exception {
        dispatchService.handleDownlink(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO>>() {}));
    }
}
