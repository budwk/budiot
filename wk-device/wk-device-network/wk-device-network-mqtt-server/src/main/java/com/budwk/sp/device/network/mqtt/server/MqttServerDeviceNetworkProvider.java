package com.budwk.sp.device.network.mqtt.server;

import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.DeviceGatewayBinding;
import com.budwk.sp.device.network.DeviceNetworkDownlinkMessage;
import com.budwk.sp.device.network.DeviceNetworkHandler;
import com.budwk.sp.device.network.DeviceNetworkInboundMessage;
import com.budwk.sp.device.network.DeviceNetworkProvider;
import com.budwk.sp.device.network.DeviceNetworkServer;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttAuth;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.MqttWill;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Component
public class MqttServerDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.MQTT;
    }

    @Override
    public boolean supports(DeviceGatewayBinding binding) {
        return binding != null && binding.getProtocol() == DeviceNetworkProtocol.MQTT && binding.getGatewayMode() == DeviceGatewayMode.MQTT_SERVER;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new MqttServerImpl(binding, handler, vertx);
    }

    static class MqttServerImpl implements DeviceNetworkServer {
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();
        private final Map<String, Map<String, MqttQoS>> persistentSubscriptions = new ConcurrentHashMap<>();
        private final Map<String, RetainedMessage> retainedMessages = new ConcurrentHashMap<>();
        private MqttServer server;
        private volatile boolean running;

        MqttServerImpl(DeviceGatewayBinding binding, DeviceNetworkHandler handler, Vertx vertx) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            server = MqttServer.create(vertx, new MqttServerOptions().setHost(binding.getHost()).setPort(binding.getPort()));
            CompletableFuture<Void> future = new CompletableFuture<>();
            server.endpointHandler(this::handleEndpoint).listen(ar -> {
                if (ar.succeeded()) future.complete(null);
                else future.completeExceptionally(ar.cause());
            });
            future.get();
            running = true;
        }

        private void handleEndpoint(MqttEndpoint endpoint) {
            String clientId = Strings.sBlank(endpoint.clientIdentifier(), UUID.randomUUID().toString().replace("-", ""));
            if (!isClientIdAllowed(clientId)) {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                return;
            }
            if (!authenticate(endpoint.auth())) {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD);
                return;
            }
            ClientSession oldSession = sessions.remove(clientId);
            if (oldSession != null) {
                oldSession.gracefulDisconnect.set(true);
                oldSession.endpoint.close();
            }
            ClientSession session = new ClientSession(clientId, endpoint, endpoint.will(), endpoint.isCleanSession(), endpoint.auth());
            if (session.cleanSession) {
                persistentSubscriptions.remove(clientId);
            } else {
                Map<String, MqttQoS> savedSubscriptions = persistentSubscriptions.get(clientId);
                if (savedSubscriptions != null) session.subscriptions.putAll(savedSubscriptions);
            }
            sessions.put(clientId, session);
            endpoint.publishAutoAck(false);
            endpoint.subscriptionAutoAck(false);
            endpoint.autoKeepAlive(true);
            endpoint.disconnectHandler(v -> closeSession(session, false, null));
            endpoint.disconnectMessageHandler(v -> {
                session.gracefulDisconnect.set(true);
                closeSession(session, false, null);
            });
            endpoint.closeHandler(v -> closeSession(session, false, null));
            endpoint.exceptionHandler(ex -> closeSession(session, true, ex));
            endpoint.subscribeHandler(message -> handleSubscribe(session, message));
            endpoint.unsubscribeHandler(message -> handleUnsubscribe(session, message));
            endpoint.publishHandler(message -> handlePublish(session, message));
            endpoint.publishReleaseHandler(endpoint::publishComplete);
            endpoint.accept(false);
            replayRetainedMessages(session, session.subscriptions.keySet());
        }

        private boolean authenticate(MqttAuth auth) {
            if (binding.isAllowAnonymous() && Strings.isBlank(binding.getUsername())) return true;
            if (Strings.isBlank(binding.getUsername())) return true;
            if (auth == null) return false;
            return binding.getUsername().equals(auth.getUsername()) && Strings.sBlank(binding.getPassword(), "").equals(Strings.sBlank(auth.getPassword(), ""));
        }

        private boolean isClientIdAllowed(String clientId) {
            return Strings.isBlank(binding.getClientIdPrefix()) || clientId.startsWith(binding.getClientIdPrefix());
        }

        private void handleSubscribe(ClientSession session, MqttSubscribeMessage message) {
            List<MqttQoS> granted = new ArrayList<>();
            List<String> subscribedTopics = new ArrayList<>();
            for (MqttTopicSubscription subscription : message.topicSubscriptions()) {
                session.subscriptions.put(subscription.topicName(), subscription.qualityOfService());
                granted.add(subscription.qualityOfService());
                subscribedTopics.add(subscription.topicName());
            }
            persistSubscriptions(session);
            session.endpoint.subscribeAcknowledge(message.messageId(), granted);
            replayRetainedMessages(session, subscribedTopics);
        }

        private void handleUnsubscribe(ClientSession session, MqttUnsubscribeMessage message) {
            for (String topic : message.topics()) session.subscriptions.remove(topic);
            persistSubscriptions(session);
            session.endpoint.unsubscribeAcknowledge(message.messageId());
        }

        private void handlePublish(ClientSession session, MqttPublishMessage publishMessage) {
            if (publishMessage.isRetain()) {
                if (publishMessage.payload() == null || publishMessage.payload().length() == 0) retainedMessages.remove(publishMessage.topicName());
                else retainedMessages.put(publishMessage.topicName(), new RetainedMessage(publishMessage.topicName(), publishMessage.payload(), publishMessage.qosLevel()));
            }
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(binding.getTenantId());
            message.setDeviceCode(session.clientId);
            message.setSessionId(session.clientId);
            message.setRemoteAddress(session.endpoint.remoteAddress() != null ? session.endpoint.remoteAddress().toString() : "");
            message.setEndpoint(publishMessage.topicName());
            message.setPayload(publishMessage.payload().toString(StandardCharsets.UTF_8));
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().put("topic", publishMessage.topicName());
            message.getHeaders().put("qos", String.valueOf(publishMessage.qosLevel().value()));
            message.getHeaders().put("retain", String.valueOf(publishMessage.isRetain()));
            message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
            handler.onMessage(message);
            publishMessage.ack();
        }

        private void replayRetainedMessages(ClientSession session, Iterable<String> topics) {
            for (String subscriptionTopic : topics) {
                retainedMessages.values().stream().filter(retained -> topicMatches(subscriptionTopic, retained.topic))
                        .forEach(retained -> session.endpoint.publish(retained.topic, retained.payload, retained.qos, true, false));
            }
        }

        private void closeSession(ClientSession session, boolean fromException, Throwable error) {
            if (!session.closed.compareAndSet(false, true)) return;
            sessions.remove(session.clientId, session);
            if (session.cleanSession) persistentSubscriptions.remove(session.clientId);
            else persistSubscriptions(session);
            if (!session.gracefulDisconnect.get()) emitWillMessage(session, fromException, error);
        }

        private void emitWillMessage(ClientSession session, boolean fromException, Throwable error) {
            MqttWill will = session.will;
            if (will == null || !will.isWillFlag()) return;
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(binding.getTenantId());
            message.setDeviceCode(session.clientId);
            message.setSessionId(session.clientId);
            message.setRemoteAddress(session.endpoint.remoteAddress() != null ? session.endpoint.remoteAddress().toString() : "");
            message.setEndpoint(will.getWillTopic());
            message.setPayload(will.getWillMessage() == null ? "" : will.getWillMessage().toString(StandardCharsets.UTF_8));
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().put("topic", will.getWillTopic());
            message.getHeaders().put("messageType", "WILL");
            message.getHeaders().put("disconnectSource", fromException ? "EXCEPTION" : "SOCKET_CLOSE");
            message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
            if (error != null && Strings.isNotBlank(error.getMessage())) message.getHeaders().put("error", error.getMessage());
            handler.onMessage(message);
        }

        private void persistSubscriptions(ClientSession session) {
            if (!session.cleanSession) persistentSubscriptions.put(session.clientId, new ConcurrentHashMap<>(session.subscriptions));
        }

        private boolean topicMatches(String subscriptionTopic, String actualTopic) {
            if (Strings.isBlank(subscriptionTopic) || Strings.isBlank(actualTopic)) return false;
            String regex = Pattern.quote(subscriptionTopic).replace("\\+", "[^/]+").replace("\\#", ".+");
            return actualTopic.matches(regex);
        }

        @Override
        public void stop() {
            running = false;
            if (server != null) server.close();
            sessions.values().forEach(session -> {
                session.gracefulDisconnect.set(true);
                session.endpoint.close();
            });
            sessions.clear();
        }

        @Override public boolean isRunning() { return running; }

        @Override
        public void send(DeviceNetworkDownlinkMessage message) {
            ClientSession session = sessions.get(message.getDeviceCode());
            if (session == null || !session.endpoint.isConnected()) return;
            String topic = Strings.sBlank(message.getTopic(), binding.getDownlinkTopicPrefix() + message.getDeviceCode());
            session.endpoint.publish(topic, Buffer.buffer(Strings.sBlank(message.getPayload(), "")), MqttQoS.AT_LEAST_ONCE, false, false);
        }

        static class ClientSession {
            private final String clientId;
            private final MqttEndpoint endpoint;
            private final MqttWill will;
            private final boolean cleanSession;
            private final MqttAuth auth;
            private final Map<String, MqttQoS> subscriptions = new ConcurrentHashMap<>();
            private final AtomicBoolean gracefulDisconnect = new AtomicBoolean(false);
            private final AtomicBoolean closed = new AtomicBoolean(false);

            ClientSession(String clientId, MqttEndpoint endpoint, MqttWill will, boolean cleanSession, MqttAuth auth) {
                this.clientId = clientId;
                this.endpoint = endpoint;
                this.will = will;
                this.cleanSession = cleanSession;
                this.auth = auth;
            }
        }

        static class RetainedMessage {
            private final String topic;
            private final Buffer payload;
            private final MqttQoS qos;

            RetainedMessage(String topic, Buffer payload, MqttQoS qos) {
                this.topic = topic;
                this.payload = payload;
                this.qos = qos;
            }
        }
    }
}
