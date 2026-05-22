package com.budwk.sp.device.network.udp;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.*;
import com.budwk.sp.device.network.support.JsonPayloadSupport;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UdpDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.UDP;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new UdpServer(binding, handler, vertx);
    }

    static class UdpServer implements DeviceNetworkServer {
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private final Map<String, io.vertx.core.net.SocketAddress> addresses = new ConcurrentHashMap<>();
        private DatagramSocket socket;
        private volatile boolean running;

        UdpServer(DeviceGatewayBinding binding, DeviceNetworkHandler handler, Vertx vertx) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            socket = vertx.createDatagramSocket();
            CompletableFuture<Void> future = new CompletableFuture<>();
            socket.handler(this::handlePacket);
            socket.listen(binding.getPort(), binding.getHost(), ar -> {
                if (ar.succeeded()) future.complete(null);
                else future.completeExceptionally(ar.cause());
            });
            future.get();
            running = true;
        }

        private void handlePacket(DatagramPacket packet) {
            String payload = packet.data().toString();
            Map<String, String> parsed = JsonPayloadSupport.extract(payload);
            String deviceCode = Strings.sBlank(parsed.get("deviceCode"), UUID.randomUUID().toString().replace("-", ""));
            addresses.put(deviceCode, packet.sender());
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(Strings.sBlank(parsed.get("tenantId"), binding.getTenantId()));
            message.setProductKey(Strings.sBlank(parsed.get("productKey"), binding.getProductKey()));
            message.setDeviceCode(deviceCode);
            message.setSessionId(deviceCode);
            message.setRemoteAddress(packet.sender() != null ? packet.sender().toString() : "");
            message.setEndpoint(binding.getHost() + ":" + binding.getPort());
            message.setPayload(payload);
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().putAll(parsed);
            message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
            handler.onMessage(message);
        }

        @Override
        public void stop() {
            running = false;
            if (socket != null) socket.close();
            addresses.clear();
        }

        @Override public boolean isRunning() { return running; }

        @Override
        public void send(DeviceNetworkDownlinkMessage message) {
            io.vertx.core.net.SocketAddress address = addresses.get(message.getDeviceCode());
            if (socket != null && address != null) socket.send(Buffer.buffer(Strings.sBlank(message.getPayload(), "")), address.port(), address.host());
        }
    }
}
