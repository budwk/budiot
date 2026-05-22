package com.budwk.sp.device.database.mq;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import com.budwk.sp.device.database.service.DeviceDatabaseDispatchService;
import com.budwk.sp.device.dto.*;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.support.DeviceRedisMessageSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "REDIS")
public class RedisDatabaseConsumer {
    private final String consumerName = "database-" + UUID.randomUUID().toString().replace("-", "");
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DeviceDatabaseDispatchService dispatchService;
    private final DeviceDatabaseProperties databaseProperties;
    private final DeviceMessageProperties messageProperties;

    public RedisDatabaseConsumer(StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 DeviceDatabaseDispatchService dispatchService,
                                 DeviceDatabaseProperties databaseProperties,
                                 DeviceMessageProperties messageProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.dispatchService = dispatchService;
        this.databaseProperties = databaseProperties;
        this.messageProperties = messageProperties;
    }

    @PostConstruct
    public void init() {
        ensureGroup(databaseProperties.getRawTopic(), "wk.device.database.raw");
        ensureGroup(databaseProperties.getRawArchiveTopic(), "wk.device.database.raw.archive");
        ensureGroup(databaseProperties.getNormalizedTopic(), "wk.device.database.normalized");
        ensureGroup(databaseProperties.getEventTopic(), "wk.device.database.event");
        ensureGroup(databaseProperties.getDownlinkTopic(), "wk.device.database.downlink");
    }

    @Scheduled(fixedDelayString = "${wk.device.message.redis-block-ms:1000}")
    public void poll() {
        poll(databaseProperties.getRawTopic(), "wk.device.database.raw", new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}, dispatchService::handleRaw);
        poll(databaseProperties.getRawArchiveTopic(), "wk.device.database.raw.archive", new TypeReference<DeviceMessageEnvelope<DeviceRawMessageDTO>>() {}, dispatchService::handleRaw);
        poll(databaseProperties.getNormalizedTopic(), "wk.device.database.normalized", new TypeReference<DeviceMessageEnvelope<DeviceUplinkMessageDTO>>() {}, dispatchService::handleNormalized);
        poll(databaseProperties.getEventTopic(), "wk.device.database.event", new TypeReference<DeviceMessageEnvelope<DeviceEventMessageDTO>>() {}, dispatchService::handleEvent);
        poll(databaseProperties.getDownlinkTopic(), "wk.device.database.downlink", new TypeReference<DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO>>() {}, dispatchService::handleDownlink);
    }

    private <T> void poll(String topic, String group, TypeReference<DeviceMessageEnvelope<T>> type, java.util.function.Consumer<DeviceMessageEnvelope<T>> consumer) {
        ensureGroup(topic, group);
        String streamKey = DeviceRedisMessageSupport.streamKey(messageProperties, topic);
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(group, consumerName),
                StreamReadOptions.empty().count(Math.max(messageProperties.getRedisPollCount(), 1)).block(Duration.ofMillis(Math.max(messageProperties.getRedisBlockMs(), 200L))),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            try {
                String payload = Strings.sNull(String.valueOf(record.getValue().getOrDefault("payload", ""))).trim();
                if (Strings.isBlank(payload)) {
                    stringRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
                    continue;
                }
                
                DeviceMessageEnvelope<T> envelope = objectMapper.readValue(payload, type);
                
                // 消息幂等性检查：基于 messageId 去重
                if (isDuplicate(envelope.getMessageId(), topic)) {
                    log.warn("Duplicate message detected, skipping: messageId={}, topic={}", envelope.getMessageId(), topic);
                    stringRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
                    continue;
                }
                
                consumer.accept(envelope);
                log.info("Database 消费完成: topic={}, messageId={}, deviceCode={}",
                        topic, envelope.getMessageId(), envelope.getDeviceCode());
                
                // 标记消息已处理
                markAsProcessed(envelope.getMessageId(), topic);
                
                stringRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
            } catch (Exception e) {
                log.error("Database 消费失败: topic={}, streamKey={}, recordId={}, error={}",
                        topic, streamKey, record.getId(), Strings.sBlank(e.getMessage(), e.getClass().getName()), e);
                // 发送到死信队列
                sendToDeadLetterQueue(streamKey, topic, record, e);
                // 确认原消息，避免阻塞后续消费
                stringRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
            }
        }
    }

    /**
     * 检查消息是否重复（基于 Redis SET）
     */
    private boolean isDuplicate(String messageId, String topic) {
        if (Strings.isBlank(messageId)) {
            return false;
        }
        String key = "wk:device:msg:dedup:" + topic.replace('.', ':');
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, messageId));
    }

    /**
     * 标记消息已处理
     */
    private void markAsProcessed(String messageId, String topic) {
        if (Strings.isBlank(messageId)) {
            return;
        }
        String key = "wk:device:msg:dedup:" + topic.replace('.', ':');
        stringRedisTemplate.opsForSet().add(key, messageId);
        // 设置过期时间（24 小时），避免无限增长
        stringRedisTemplate.expire(key, Duration.ofHours(24));
    }

    /**
     * 发送失败消息到死信队列
     */
    private void sendToDeadLetterQueue(String streamKey, String topic, MapRecord<String, Object, Object> record, Exception error) {
        try {
            String deadLetterStreamKey = streamKey + ":dead";
            Map<String, String> deadLetter = new LinkedHashMap<>();
            deadLetter.put("originalStream", streamKey);
            deadLetter.put("originalTopic", topic);
            deadLetter.put("originalMessageId", Strings.sNull(record.getId()).trim());
            deadLetter.put("payload", Strings.sNull(String.valueOf(record.getValue().getOrDefault("payload", ""))).trim());
            deadLetter.put("errorMessage", Strings.sBlank(error.getMessage(), "Unknown error").substring(0, Math.min(error.getMessage() != null ? error.getMessage().length() : 0, 500)));
            deadLetter.put("errorClass", error.getClass().getName());
            deadLetter.put("failedAt", String.valueOf(System.currentTimeMillis()));
            stringRedisTemplate.opsForStream().add(deadLetterStreamKey, deadLetter);
        } catch (Exception logError) {
            // 死信队列写入失败时，打印日志但不影响主流程
            System.err.println("Failed to send message to dead letter queue: " + logError.getMessage());
        }
    }

    private void ensureGroup(String topic, String group) {
        DeviceRedisMessageSupport.ensureGroup(stringRedisTemplate, DeviceRedisMessageSupport.streamKey(messageProperties, topic), group);
    }
}
