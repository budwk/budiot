package com.budwk.sp.starter.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "wk.redis")
public class WkRedisProperties {

    /**
     * 全局缓存前缀
     */
    private String prefix = "wk:";

    /**
     * 默认过期时间（默认 1 小时）
     */
    private Duration defaultTtl = Duration.ofHours(1);

    /**
     * 是否允许缓存空值（防止缓存穿透）
     */
    private boolean cacheNullValues = false;

    /**
     * 针对特定 CacheName 的个性化过期时间
     * 例如：wk.redis.custom-ttl.auth_tokens=7d
     */
    private Map<String, Duration> customTtl;
}