package com.budwk.sp.device.network.mqtt.client;

import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.DeviceGatewayBinding;
import com.budwk.sp.device.network.DeviceNetworkDownlinkMessage;
import com.budwk.sp.device.network.DeviceNetworkHandler;
import com.budwk.sp.device.network.DeviceNetworkInboundMessage;
import com.budwk.sp.device.network.DeviceNetworkProvider;
import com.budwk.sp.device.network.DeviceNetworkServer;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MqttClientDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.MQTT;
    }

    @Override
    public boolean supports(DeviceGatewayBinding binding) {
        return binding != null && binding.getProtocol() == DeviceNetworkProtocol.MQTT && binding.getGatewayMode() == DeviceGatewayMode.MQTT_CLIENT;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new MqttClientServer(binding, handler, vertx);
    }

    static class MqttClientServer implements DeviceNetworkServer {
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private MqttClient client;
        private volatile boolean running;
        private final AtomicBoolean reconnecting = new AtomicBoolean(false);

        MqttClientServer(DeviceGatewayBinding binding, DeviceNetworkHandler handler, Vertx vertx) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            connect().get();
            running = true;
        }

        private CompletableFuture<Void> connect() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            MqttClientOptions options = new MqttClientOptions()
                    .setClientId(Strings.sBlank(binding.getClientId(), "gw-" + UUID.randomUUID().toString().replace("-", "")))
                    .setAutoKeepAlive(true)
                    .setCleanSession(true);
            if (Strings.isNotBlank(binding.getUsername())) options.setUsername(binding.getUsername());
            if (Strings.isNotBlank(binding.getPassword())) options.setPassword(binding.getPassword());
            client = MqttClient.create(vertx, options);
            client.publishHandler(this::handlePublish);
            client.closeHandler(v -> tryReconnect());
            client.exceptionHandler(ex -> tryReconnect());
            client.connect(binding.getRemotePort() == null ? 1883 : binding.getRemotePort(), Strings.sBlank(binding.getRemoteHost(), "127.0.0.1"), ar -> {
                if (ar.failed()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                subscribeConfiguredTopics();
                reconnecting.set(false);
                future.complete(null);
            });
            return future;
        }

        private void subscribeConfiguredTopics() {
            Map<String, Integer> topics = new LinkedHashMap<>();
            for (String topic : Strings.sBlank(binding.getSubscribeTopic(), "up/#").split(",")) {
                String resolved = Strings.sNull(topic).trim();
                if (Strings.isNotBlank(resolved)) topics.put(resolved, 1);
            }
            if (!topics.isEmpty()) client.subscribe(topics);
        }

        private void handlePublish(MqttPublishMessage publishMessage) {
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(binding.getTenantId());
            message.setDeviceCode(resolveDeviceCode(publishMessage));
            message.setSessionId(client.clientId());
            message.setRemoteAddress(Strings.sBlank(binding.getRemoteHost(), ""));
            message.setEndpoint(publishMessage.topicName());
            message.setPayload(publishMessage.payload().toString(StandardCharsets.UTF_8));
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().put("topic", publishMessage.topicName());
            message.getHeaders().put("qos", String.valueOf(publishMessage.qosLevel().value()));
            message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
            handler.onMessage(message);
        }

        private String resolveDeviceCode(MqttPublishMessage publishMessage) {
            String[] parts = Strings.sBlank(publishMessage.topicName(), "").split("/");
            return parts.length > 1 ? parts[parts.length - 1] : Strings.sBlank(client.clientId(), "mqtt-client");
        }

        private void tryReconnect() {
            if (!running || !reconnecting.compareAndSet(false, true)) return;
            vertx.setTimer(3000, timer -> connect().whenComplete((v, ex) -> reconnecting.set(false)));
        }

        @Override
        public void stop() throws Exception {
            running = false;
            reconnecting.set(false);
            if (client != null && client.isConnected()) {
                client.disconnect().toCompletionStage().toCompletableFuture().get();
            }
        }

        @Override public boolean isRunning() { return running; }

        @Override
        public void send(DeviceNetworkDownlinkMessage message) {
            if (client == null || !client.isConnected()) return;
            String topic = Strings.sBlank(message.getTopic(), Strings.sBlank(binding.getPublishTopic(), "cmd/") + message.getDeviceCode());
            client.publish(topic, Buffer.buffer(Strings.sBlank(message.getPayload(), "")), MqttQoS.AT_LEAST_ONCE, false, false);
        }
    }
}
