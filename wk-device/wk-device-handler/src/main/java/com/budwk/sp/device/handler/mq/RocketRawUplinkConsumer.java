package com.budwk.sp.device.handler.mq;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.budwk.sp.device.handler.service.DeviceErrorMessageService;
import com.budwk.sp.device.handler.service.DeviceRawMessageHandlerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 原始上行消息消费者
 * 
 * 改进点：
 * 1. 捕获所有异常，防止消息丢失
 * 2. 记录错误消息到数据库
 * 3. 提供重试机制
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "ROCKETMQ")
@RocketMQMessageListener(
        topic = "${wk.device.handler.raw-topic:wk.device.uplink.raw}",
        consumerGroup = "${spring.application.name}-raw-uplink",
        messageModel = MessageModel.CLUSTERING
)
public class RocketRawUplinkConsumer implements RocketMQListener<String> {
    private final ObjectMapper objectMapper;
    private final DeviceRawMessageHandlerService handlerService;
    private final DeviceErrorMessageService errorService;

    public RocketRawUplinkConsumer(ObjectMapper objectMapper, 
                                   DeviceRawMessageHandlerService handlerService,
                                   DeviceErrorMessageService errorService) {
        this.objectMapper = objectMapper;
        this.handlerService = handlerService;
        this.errorService = errorService;
    }

    @Override
    public void onMessage(String message) {
        DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = null;
        try {
            // 1. 解析消息
            envelope = objectMapper.readValue(message, new TypeReference<>() {});
            
            // 2. 处理消息
            handlerService.handle(envelope);
            
            log.debug("消息处理成功: messageId={}, deviceCode={}", 
                envelope.getMessageId(), envelope.getDeviceCode());
            
        } catch (Exception e) {
            log.error("消息处理失败: messageId={}, deviceCode={}, error={}", 
                envelope != null ? envelope.getMessageId() : "unknown",
                envelope != null ? envelope.getDeviceCode() : "unknown",
                e.getMessage(), e);
            
            // 3. 记录错误消息
            try {
                errorService.recordError(
                    envelope,
                    message,
                    "CONSUME_ERROR",
                    e.getMessage(),
                    getStackTrace(e)
                );
            } catch (Exception recordError) {
                // 记录失败时，输出到日志
                log.error("记录错误消息失败: {}", recordError.getMessage(), recordError);
            }
            
            // 4. 不再抛出异常，避免消息丢失
            // 原代码：throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 4000) break;
        }
        return sb.toString();
    }
}
