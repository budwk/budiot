package com.budwk.sp.device.rule.mq;

import com.budwk.sp.device.dto.DeviceEventMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceUplinkMessageDTO;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.support.DeviceRedisMessageSupport;
import com.budwk.sp.device.rule.config.DeviceRuleProperties;
import com.budwk.sp.device.rule.service.DeviceRuleEngineService;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "REDIS")
public class RedisRuleConsumer {
    private final String consumerName = "rule-" + UUID.randomUUID().toString().replace("-", "");
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DeviceRuleEngineService engineService;
    private final DeviceRuleProperties ruleProperties;
    private final DeviceMessageProperties messageProperties;

    public RedisRuleConsumer(StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper,
                             DeviceRuleEngineService engineService,
                             DeviceRuleProperties ruleProperties,
                             DeviceMessageProperties messageProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.engineService = engineService;
        this.ruleProperties = ruleProperties;
        this.messageProperties = messageProperties;
    }

    @PostConstruct
    public void init() {
        ensureGroup(ruleProperties.getNormalizedTopic(), "wk.device.rule.normalized");
        ensureGroup(ruleProperties.getEventTopic(), "wk.device.rule.event");
    }

    @Scheduled(fixedDelayString = "${wk.device.message.redis-block-ms:1000}")
    public void poll() {
        poll(ruleProperties.getNormalizedTopic(), "wk.device.rule.normalized", new TypeReference<DeviceMessageEnvelope<DeviceUplinkMessageDTO>>() {}, engineService::handleNormalized);
        poll(ruleProperties.getEventTopic(), "wk.device.rule.event", new TypeReference<DeviceMessageEnvelope<DeviceEventMessageDTO>>() {}, engineService::handleEvent);
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
                consumer.accept(envelope);
                log.info("Rule 消费完成: topic={}, messageId={}, deviceCode={}",
                        topic, envelope.getMessageId(), envelope.getDeviceCode());
                stringRedisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
            } catch (Exception e) {
                throw new IllegalStateException(Strings.sBlank(e.getMessage(), "Redis 规则消费失败"), e);
            }
        }
    }

    private void ensureGroup(String topic, String group) {
        DeviceRedisMessageSupport.ensureGroup(stringRedisTemplate, DeviceRedisMessageSupport.streamKey(messageProperties, topic), group);
    }
}
