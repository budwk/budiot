package com.budwk.sp.starter.cache.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory.class)
@EnableConfigurationProperties(CacheProperties.class) // 注入原生 Cache 配置
public class WkCacheAutoConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        // 设置 Key 的序列化方式为 String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          CacheProperties cacheProperties) {
        // 获取 yml 中 spring.cache.redis 的配置
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();

        // 1. 基础配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        // 2. 使用 Redis 内置的带类型信息 JSON 序列化器，确保 @Cacheable 命中后能反序列化回实体对象
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        // 3. 序列化配置 (防止乱码)
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 3. 关联原生配置项
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
