package com.budwk.sp.sys.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 平台站内信 WebSocket 处理器
 */
@Slf4j
@Component
public class WkWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final WkWebSocketSessionRegistry sessionRegistry;
    private final ISysMsgProvider sysMsgProvider;

    public WkWebSocketHandler(ObjectMapper objectMapper,
                              WkWebSocketSessionRegistry sessionRegistry,
                              ISysMsgProvider sysMsgProvider) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
        this.sysMsgProvider = sysMsgProvider;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) {
            return;
        }
        String text = payload.trim();
        if ("{}".equals(text)) {
            return;
        }
        WsCommand command = objectMapper.readValue(text, WsCommand.class);
        if (command == null || !StringUtils.hasText(command.action())) {
            return;
        }
        if ("join".equalsIgnoreCase(command.action())) {
            handleJoin(session, command);
            return;
        }
        if ("left".equalsIgnoreCase(command.action())) {
            sessionRegistry.unregister(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleJoin(WebSocketSession session, WsCommand command) throws Exception {
        if (!StringUtils.hasText(command.userId()) || !StringUtils.hasText(command.token())) {
            sendOfflineAndClose(session);
            return;
        }
        Object loginId = StpUtil.getLoginIdByToken(command.token());
        if (loginId == null || !command.userId().equals(String.valueOf(loginId))) {
            sendOfflineAndClose(session);
            return;
        }
        sessionRegistry.register(command.userId(), command.token(), session);
        sysMsgProvider.getMsg(command.userId(), false);
    }

    private void sendOfflineAndClose(WebSocketSession session) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"action\":\"offline\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
        }
        sessionRegistry.unregister(session);
    }

    private record WsCommand(String action, String userId, String token) {
    }
}
