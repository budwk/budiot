package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "KAFKA")
public class KafkaDatabaseConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;

    public KafkaDatabaseConsumer(ObjectMapper objectMapper, DeviceDatabaseDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = "${wk.device.database-ext.raw-topic:wk.device.uplink.raw}", groupId = "${spring.application.name}-raw")
    public void onRaw(String message) throws Exception {
        dispatchService.handleRaw(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}));
    }

    @KafkaListener(topics = "${wk.device.database-ext.raw-archive-topic:wk.device.uplink.raw.archive}", groupId = "${spring.application.name}-raw-archive")
    public void onRawArchive(String message) throws Exception {
        dispatchService.handleRaw(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}));
    }

    @KafkaListener(topics = "${wk.device.database-ext.normalized-topic:wk.device.uplink.normalized}", groupId = "${spring.application.name}-normalized")
    public void onNormalized(String message) throws Exception {
        dispatchService.handleNormalized(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceUplinkMessageDTO>>() {}));
    }

    @KafkaListener(topics = "${wk.device.database-ext.event-topic:wk.device.event}", groupId = "${spring.application.name}-event")
    public void onEvent(String message) throws Exception {
        dispatchService.handleEvent(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceEventMessageDTO>>() {}));
    }

    @KafkaListener(topics = "${wk.device.database-ext.downlink-topic:wk.device.downlink.command}", groupId = "${spring.application.name}-downlink")
    public void onDownlink(String message) throws Exception {
        dispatchService.handleDownlink(objectMapper.readValue(message, new TypeReference<DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO>>() {}));
    }
}
