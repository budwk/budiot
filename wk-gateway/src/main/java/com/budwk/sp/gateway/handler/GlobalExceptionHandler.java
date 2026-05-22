package com.budwk.sp.gateway.handler;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        int status;
        String message;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND.value();
            message = "服务未找到";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            status = responseStatusException.getStatusCode().value();
            message = responseStatusException.getReason();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            message = "网关内部错误";
            log.error("Gateway error: ", ex);
        }

        response.setStatusCode(HttpStatus.valueOf(status));

        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("msg", message);
        result.put("traceId", exchange.getRequest().getHeaders().getFirst("X-Trace-Id"));
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes = JSON.toJSONString(result).getBytes();
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
