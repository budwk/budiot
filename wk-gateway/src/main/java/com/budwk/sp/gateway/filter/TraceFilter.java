package com.budwk.sp.gateway.filter;

import com.budwk.sp.gateway.config.WkGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪过滤器
 * 生成 TraceId 并传递给下游服务
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final WkGatewayProperties gatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.getTrace().isEnabled()) {
            return chain.filter(exchange);
        }

        // 获取或生成 TraceId
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // 将 TraceId 添加到请求头
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        // 将 TraceId 添加到响应头
        if (gatewayProperties.getTrace().isResponseHeader()) {
            exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        }

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
