package com.budwk.sp.starter.cache.service;

import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WkCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final String prefix;

    public WkCacheService(RedisTemplate<String, Object> redisTemplate,
                          StringRedisTemplate stringRedisTemplate,
                          CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        // 从原生配置 spring.cache.redis.key-prefix 获取前缀
        String configPrefix = cacheProperties.getRedis().getKeyPrefix();
        this.prefix = StringUtils.hasText(configPrefix) ? configPrefix : "";
    }

    /**
     * 统一处理 Key 前缀
     */
    private String buildKey(String key) {
        if (!StringUtils.hasText(prefix)) {
            return key;
        }
        // 如果传入的 key 已经带了前缀，则不再重复添加
        return key.startsWith(prefix) ? key : prefix + key;
    }
    /**
     * 手动设置缓存 (带过期时间)
     */
    public void setCache(String key, Object value, long timeout, TimeUnit unit) {
        // opsForValue() 专门处理 String 类型的 Value 操作
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 读取普通 KV 缓存值。
     */
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定 Key 的缓存数据。
     */
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 写入 Redis Hash 的单个字段值，适合设备运行态这类结构化小对象。
     */
    public void putHashValue(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 确保 Stream 消费组存在；若 Stream 还未初始化，会先写入一条占位消息。
     */
    public void ensureStreamGroup(String streamKey, String group) {
        StreamOperations<String, Object, Object> ops = stringRedisTemplate.opsForStream();
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey))) {
            Map<String, String> init = new LinkedHashMap<>();
            init.put("_init", "1");
            ops.add(MapRecord.create(streamKey, init));
        }
        try {
            ops.createGroup(streamKey, ReadOffset.latest(), group);
        } catch (Exception ignored) {
        }
    }

    /**
     * 以消费组模式读取 Stream 消息，封装阻塞时长和批量条数控制。
     */
    public List<MapRecord<String, Object, Object>> readStream(String streamKey,
                                                              String group,
                                                              String consumer,
                                                              long count,
                                                              Duration block) {
        return stringRedisTemplate.opsForStream().read(
                Consumer.from(group, consumer),
                StreamReadOptions.empty().count(Math.max(count, 1)).block(block),
                org.springframework.data.redis.connection.stream.StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
    }

    /**
     * 确认消费组中的某条 Stream 消息已处理完成。
     */
    public Long acknowledgeStream(String streamKey, String group, RecordId recordId) {
        return stringRedisTemplate.opsForStream().acknowledge(streamKey, group, recordId);
    }
}
