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

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 请求日志过滤器
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "gatewayStartTime";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final WkGatewayProperties gatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.getLog().isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // 记录请求开始时间
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        // 获取 TraceId
        String traceId = request.getHeaders().getFirst("X-Trace-Id");

        // 记录请求日志
        if (log.isInfoEnabled()) {
            String requestTime = LocalDateTime.now().format(FORMATTER);
            String method = request.getMethod().name();
            String path = request.getPath().value();
            String query = request.getURI().getQuery();
            String clientIp = getClientIp(request);

            log.info("[Gateway] {} | {} | {} {} | {}",
                    requestTime,
                    traceId != null ? traceId : "-",
                    method,
                    path + (query != null ? "?" + query : ""),
                    clientIp);
        }

        // 响应完成后记录响应时间
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime != null && log.isInfoEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;

                log.info("[Gateway] {} | {}ms | {}",
                        traceId != null ? traceId : "-",
                        duration,
                        statusCode);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null) {
                ip = remoteAddress.getAddress().getHostAddress();
            }
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
