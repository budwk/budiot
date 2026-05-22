package com.budwk.sp.sys.websocket;

import com.budwk.sp.starter.common.constant.RedisConstant;
import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 版 OAuth state cache
 */
@Component
public class RedisAuthStateCache implements AuthStateCache {
    private static final long DEFAULT_TIMEOUT = 10 * 60;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisAuthStateCache(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void cache(String key, String value) {
        cache(key, value, DEFAULT_TIMEOUT);
    }

    @Override
    public void cache(String key, String value, long timeout) {
        stringRedisTemplate.opsForValue().set(RedisConstant.UCENTER_OAUTH_STATE + key, value, timeout, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(RedisConstant.UCENTER_OAUTH_STATE + key);
    }

    @Override
    public boolean containsKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstant.UCENTER_OAUTH_STATE + key));
    }
}
