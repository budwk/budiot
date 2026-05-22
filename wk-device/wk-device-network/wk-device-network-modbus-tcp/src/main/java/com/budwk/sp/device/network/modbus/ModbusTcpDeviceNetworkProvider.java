package com.budwk.sp.device.network.modbus;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModbusTcpDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.MODBUS_TCP;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new ModbusServer(binding, handler, vertx);
    }

    static class ModbusServer implements DeviceNetworkServer {
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private final Map<String, NetSocket> sessions = new ConcurrentHashMap<>();
        private NetServer server;
        private volatile boolean running;

        ModbusServer(DeviceGatewayBinding binding, DeviceNetworkHandler handler, Vertx vertx) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            server = vertx.createNetServer();
            CompletableFuture<Void> future = new CompletableFuture<>();
            server.connectHandler(socket -> socket.handler(buffer -> handleBuffer(socket, buffer))).listen(binding.getPort(), binding.getHost(), ar -> {
                if (ar.succeeded()) future.complete(null);
                else future.completeExceptionally(ar.cause());
            });
            future.get();
            running = true;
        }

        private void handleBuffer(NetSocket socket, Buffer buffer) {
            if (buffer.length() < 7) return;
            String deviceCode = String.valueOf(buffer.getUnsignedByte(6));
            sessions.put(deviceCode, socket);
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(binding.getTenantId());
            message.setProductKey(binding.getProductKey());
            message.setDeviceCode(deviceCode);
            message.setSessionId(deviceCode);
            message.setRemoteAddress(socket.remoteAddress() != null ? socket.remoteAddress().toString() : "");
            message.setEndpoint(binding.getHost() + ":" + binding.getPort());
            message.setPayload(bytesToHex(buffer.getBytes()));
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().put("protocolId", binding.getProtocolId() == null ? "" : binding.getProtocolId());
            handler.onMessage(message);
        }

        @Override
        public void stop() {
            running = false;
            if (server != null) server.close();
            sessions.values().forEach(NetSocket::close);
            sessions.clear();
        }

        @Override public boolean isRunning() { return running; }

        @Override
        public void send(DeviceNetworkDownlinkMessage message) {
            NetSocket socket = sessions.get(message.getDeviceCode());
            if (socket != null) socket.write(Buffer.buffer(hexToBytes(message.getPayload())));
        }

        private byte[] hexToBytes(String hex) {
            String resolved = hex == null ? "" : hex.replace(" ", "");
            if (resolved.length() % 2 != 0) resolved = "0" + resolved;
            byte[] bytes = new byte[resolved.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte) Integer.parseInt(resolved.substring(index, index + 2), 16);
            }
            return bytes;
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02X", b));
            }
            return builder.toString();
        }
    }
}
