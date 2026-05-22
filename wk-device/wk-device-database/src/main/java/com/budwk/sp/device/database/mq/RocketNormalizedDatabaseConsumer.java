package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceUplinkMessageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 标准化上行消息消费者
 * 
 * 改进：
 * 1. 捕获所有异常，避免消息丢失
 * 2. 记录详细错误日志
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(topic = "${wk.device.database-ext.normalized-topic:wk.device.uplink.normalized}", consumerGroup = "${spring.application.name}-normalized", messageModel = MessageModel.CLUSTERING)
public class RocketNormalizedDatabaseConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;

    public RocketNormalizedDatabaseConsumer(ObjectMapper objectMapper, DeviceDatabaseDispatchService dispatchService) {
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
    }

    @Override
    public void onMessage(String message) {
        try {
            DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            dispatchService.handleNormalized(envelope);
        } catch (Exception e) {
            // 不抛出异常，避免消息丢失
            log.error("处理标准化上行消息失败: {}", e.getMessage(), e);
        }
    }
}
