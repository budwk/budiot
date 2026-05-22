package com.budwk.sp.sys.websocket;

import com.budwk.sp.starter.common.constant.RedisConstant;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话注册表
 */
@Slf4j
@Component
public class WkWebSocketSessionRegistry {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WkWebSocketSessionRegistry.class);
    public static final String WS_CHANNEL = RedisConstant.PRE + "wk:websocket:message";
    private final Map<String, SessionHolder> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public WkWebSocketSessionRegistry(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void register(String userId, String token, WebSocketSession session) {
        unregister(session);
        sessions.put(session.getId(), new SessionHolder(userId, token, session));
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public void unregister(WebSocketSession session) {
        if (session != null) {
            unregister(session.getId());
        }
    }

    public void unregister(String sessionId) {
        SessionHolder holder = sessions.remove(sessionId);
        if (holder == null) {
            return;
        }
        Set<String> sessionIds = userSessions.get(holder.userId());
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                userSessions.remove(holder.userId());
            }
        }
    }

    public void sendToUser(String userId, String payload) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(payload)) {
            return;
        }
        publish(DispatchMessage.user(userId, payload));
    }

    public void sendToUserToken(String userId, String token, String payload) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(token) || !StringUtils.hasText(payload)) {
            return;
        }
        publish(DispatchMessage.userToken(userId, token, payload));
    }

    public void sendOffline(String userId, String currentToken, String payload) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(payload)) {
            return;
        }
        publish(DispatchMessage.offline(userId, currentToken, payload));
    }

    public void handleDispatchMessage(String payload) {
        if (!StringUtils.hasText(payload)) {
            return;
        }
        try {
            DispatchMessage message = objectMapper.readValue(payload, DispatchMessage.class);
            if (message == null || !StringUtils.hasText(message.userId()) || !StringUtils.hasText(message.payload())) {
                return;
            }
            switch (message.type()) {
                case USER -> doSendToUser(message.userId(), message.payload());
                case USER_TOKEN -> doSendToUserToken(message.userId(), message.token(), message.payload());
                case OFFLINE -> doSendOffline(message.userId(), message.token(), message.payload());
            }
        } catch (Exception e) {
            log.warn("handle websocket dispatch payload error", e);
        }
    }

    private List<SessionHolder> getUserSessions(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        List<SessionHolder> holders = new ArrayList<>();
        if (sessionIds == null) {
            return holders;
        }
        for (String sessionId : sessionIds) {
            SessionHolder holder = sessions.get(sessionId);
            if (holder != null) {
                holders.add(holder);
            }
        }
        return holders;
    }

    private void doSendToUser(String userId, String payload) {
        for (SessionHolder holder : getUserSessions(userId)) {
            send(holder, payload);
        }
    }

    private void doSendToUserToken(String userId, String token, String payload) {
        for (SessionHolder holder : getUserSessions(userId)) {
            if (Objects.equals(token, holder.token())) {
                send(holder, payload);
            }
        }
    }

    private void doSendOffline(String userId, String currentToken, String payload) {
        for (SessionHolder holder : getUserSessions(userId)) {
            if (StringUtils.hasText(currentToken) && Objects.equals(currentToken, holder.token())) {
                continue;
            }
            send(holder, payload);
            close(holder);
        }
    }

    private void publish(DispatchMessage message) {
        try {
            stringRedisTemplate.convertAndSend(WS_CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            throw new IllegalStateException("publish websocket message error", e);
        }
    }

    private void send(SessionHolder holder, String payload) {
        WebSocketSession session = holder.session();
        if (!session.isOpen()) {
            unregister(session);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            log.warn("send websocket message error, sessionId={}", session.getId(), e);
            unregister(session);
        }
    }

    private void close(SessionHolder holder) {
        WebSocketSession session = holder.session();
        unregister(session);
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.NORMAL);
        } catch (IOException e) {
            log.debug("close websocket session error, sessionId={}", session.getId(), e);
        }
    }

    private record SessionHolder(String userId, String token, WebSocketSession session) {
    }

    private enum DispatchType {
        USER,
        USER_TOKEN,
        OFFLINE
    }

    private record DispatchMessage(DispatchType type, String userId, String token, String payload) {
        private static DispatchMessage user(String userId, String payload) {
            return new DispatchMessage(DispatchType.USER, userId, null, payload);
        }

        private static DispatchMessage userToken(String userId, String token, String payload) {
            return new DispatchMessage(DispatchType.USER_TOKEN, userId, token, payload);
        }

        private static DispatchMessage offline(String userId, String currentToken, String payload) {
            return new DispatchMessage(DispatchType.OFFLINE, userId, currentToken, payload);
        }
    }
}
