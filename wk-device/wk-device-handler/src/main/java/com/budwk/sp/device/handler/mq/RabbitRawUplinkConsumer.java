package com.budwk.sp.device.handler.mq;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.budwk.sp.device.handler.service.DeviceRawMessageHandlerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "RABBITMQ")
public class RabbitRawUplinkConsumer {
    private final ObjectMapper objectMapper;
    private final DeviceRawMessageHandlerService handlerService;

    public RabbitRawUplinkConsumer(ObjectMapper objectMapper, DeviceRawMessageHandlerService handlerService) {
        this.objectMapper = objectMapper;
        this.handlerService = handlerService;
    }

    @RabbitListener(queues = "#{@deviceHandlerRawQueue.name}")
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            handlerService.handle(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
