package com.budwk.sp.device.network.http;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.*;
import com.budwk.sp.device.network.support.JsonPayloadSupport;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class HttpDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.HTTP;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new HttpServerImpl(binding, handler, vertx);
    }

    static class HttpServerImpl implements DeviceNetworkServer {
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private HttpServer server;
        private volatile boolean running;

        HttpServerImpl(DeviceGatewayBinding binding, DeviceNetworkHandler handler, Vertx vertx) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            server = vertx.createHttpServer();
            CompletableFuture<Void> future = new CompletableFuture<>();
            server.requestHandler(req -> req.bodyHandler(body -> {
                if (!Strings.sBlank(req.path(), "/").equals(Strings.sBlank(binding.getPath(), "/"))) {
                    req.response().setStatusCode(404).end();
                    return;
                }
                String payload = body.toString();
                Map<String, String> parsed = JsonPayloadSupport.extract(payload);
                String deviceCode = Strings.sBlank(req.getHeader("deviceCode"), parsed.get("deviceCode"));
                DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
                message.setBindingId(bindingId());
                message.setNodeId(nodeId());
                message.setProtocol(binding.getProtocol());
                message.setTenantId(Strings.sBlank(req.getHeader("tenantId"), Strings.sBlank(parsed.get("tenantId"), binding.getTenantId())));
                message.setProductKey(Strings.sBlank(req.getHeader("productKey"), Strings.sBlank(parsed.get("productKey"), binding.getProductKey())));
                message.setDeviceCode(Strings.sBlank(deviceCode, UUID.randomUUID().toString().replace("-", "")));
                message.setSessionId(message.getDeviceCode());
                message.setRemoteAddress(req.remoteAddress() != null ? req.remoteAddress().toString() : "");
                message.setEndpoint(req.path());
                message.setPayload(payload);
                message.setOccurredAt(System.currentTimeMillis());
                req.headers().forEach(header -> message.getHeaders().put(header.getKey(), header.getValue()));
                message.getHeaders().putAll(parsed);
                message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
                handler.onMessage(message);
                req.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("code", 0).put("msg", "ok").encode());
            }));
            server.listen(binding.getPort(), binding.getHost(), ar -> {
                if (ar.succeeded()) future.complete(null);
                else future.completeExceptionally(ar.cause());
            });
            future.get();
            running = true;
        }

        @Override
        public void stop() {
            running = false;
            if (server != null) server.close();
        }

        @Override public boolean isRunning() { return running; }
        @Override public void send(DeviceNetworkDownlinkMessage message) {}
    }
}
