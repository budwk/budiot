package com.budwk.sp.sys.config;

import com.budwk.sp.sys.websocket.WkWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 平台 WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WkWebSocketConfig implements WebSocketConfigurer {
    private final WkWebSocketHandler platformWebSocketHandler;

    public WkWebSocketConfig(WkWebSocketHandler platformWebSocketHandler) {
        this.platformWebSocketHandler = platformWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(platformWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}
