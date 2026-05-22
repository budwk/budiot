package com.budwk.sp.device.handler.mq;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceRawMessageDTO;
import com.budwk.sp.device.handler.service.DeviceRawMessageHandlerService;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.support.DeviceMessageTopics;
import com.budwk.sp.device.message.support.DeviceRedisMessageSupport;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "wk.device.message.provider", havingValue = "REDIS")
public class RedisRawUplinkConsumer {
    private static final String GROUP = "wk.device.handler.raw";
    private final String consumerName = "handler-" + UUID.randomUUID().toString().replace("-", "");
    private final WkCacheService wkCacheService;
    private final ObjectMapper objectMapper;
    private final DeviceRawMessageHandlerService handlerService;
    private final DeviceMessageProperties properties;

    public RedisRawUplinkConsumer(WkCacheService wkCacheService,
                                  ObjectMapper objectMapper,
                                  DeviceRawMessageHandlerService handlerService,
                                  DeviceMessageProperties properties) {
        this.wkCacheService = wkCacheService;
        this.objectMapper = objectMapper;
        this.handlerService = handlerService;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        wkCacheService.ensureStreamGroup(streamKey(), GROUP);
    }

    @Scheduled(fixedDelayString = "${wk.device.message.redis-block-ms:1000}")
    public void poll() {
        wkCacheService.ensureStreamGroup(streamKey(), GROUP);
        List<MapRecord<String, Object, Object>> records = wkCacheService.readStream(
                streamKey(),
                GROUP,
                consumerName,
                properties.getRedisPollCount(),
                Duration.ofMillis(Math.max(properties.getRedisBlockMs(), 200L))
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            try {
                String payload = Strings.sNull(String.valueOf(record.getValue().getOrDefault("payload", ""))).trim();
                if (Strings.isBlank(payload)) {
                    wkCacheService.acknowledgeStream(streamKey(), GROUP, record.getId());
                    continue;
                }
                DeviceMessageEnvelope<DeviceRawMessageDTO> envelope = objectMapper.readValue(payload, new TypeReference<>() {});
                handlerService.handle(envelope);
                log.info("Handler 原始上行处理完成: messageId={}, productKey={}, deviceCode={}",
                        envelope.getMessageId(), envelope.getProductKey(), envelope.getDeviceCode());
                wkCacheService.acknowledgeStream(streamKey(), GROUP, record.getId());
            } catch (Exception e) {
                throw new IllegalStateException(Strings.sBlank(e.getMessage(), "Redis 原始上行消费失败"), e);
            }
        }
    }

    private String streamKey() {
        return DeviceRedisMessageSupport.streamKey(properties, DeviceMessageTopics.rawUplink(properties.getTopicPrefix()));
    }
}
