package com.budwk.sp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 限流配置
 *
 * @author wizzer@qq.com
 */
@Configuration
public class RateLimitConfig {

    /**
     * IP 限流 Key 解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (ip == null || ip.isEmpty()) {
                ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            }
            if (ip == null || ip.isEmpty()) {
                if (exchange.getRequest().getRemoteAddress() != null) {
                    ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                }
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return Mono.just(ip != null ? ip : "unknown");
        };
    }

    /**
     * 用户限流 Key 解析器
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token != null && !token.isEmpty()) {
                return Mono.just("user:" + token.hashCode());
            }
            String ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (ip == null && exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just("ip:" + (ip != null ? ip : "unknown"));
        };
    }
}
