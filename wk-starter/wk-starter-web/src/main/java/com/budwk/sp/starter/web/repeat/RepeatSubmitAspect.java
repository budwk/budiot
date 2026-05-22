package com.budwk.sp.starter.web.repeat;

import com.budwk.sp.starter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Aspect
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final StringRedisTemplate redisTemplate;
    private static final String REPEAT_SUBMIT_KEY = "wk:repeat_submit:";

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        // 1. 构建指纹：这里简单演示以 userToken + URL + 请求参数作为 Key
        String userToken = request.getHeader("Authorization");
        String params = Objects.toString(point.getArgs());
        String url = request.getRequestURI();
        String rawKey = userToken + url + params;
        String md5Key = DigestUtils.md5DigestAsHex(rawKey.getBytes());
        String redisKey = REPEAT_SUBMIT_KEY + md5Key;

        // 2. 尝试存入 Redis (setIfAbsent 就是 Redis 的 setnx)
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", repeatSubmit.interval(), repeatSubmit.unit());

        if (Boolean.FALSE.equals(isAbsent)) {
            // 3. 如果存入失败，说明 Key 已存在，属于重复提交
            return Result.error(429, repeatSubmit.message());
        }

        try {
            // 4. 执行业务方法
            return point.proceed();
        } catch (Throwable throwable) {
            // 5. 如果业务执行报错，可以考虑立即删除 Key，允许用户重试
            redisTemplate.delete(redisKey);
            throw throwable;
        }
    }
}