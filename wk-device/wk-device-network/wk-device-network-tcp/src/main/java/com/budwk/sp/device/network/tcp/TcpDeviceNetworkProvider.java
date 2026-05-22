package com.budwk.sp.device.network.tcp;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.network.*;
import com.budwk.sp.device.network.support.JsonPayloadSupport;
import com.budwk.sp.device.network.tcp.config.TcpServerProperties;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpDeviceNetworkProvider implements DeviceNetworkProvider {
    private final Vertx vertx = Vertx.vertx();
    private final TcpServerProperties tcpServerProperties;

    public TcpDeviceNetworkProvider(TcpServerProperties tcpServerProperties) {
        this.tcpServerProperties = tcpServerProperties;
    }

    @Override
    public DeviceNetworkProtocol protocol() {
        return DeviceNetworkProtocol.TCP;
    }

    @Override
    public DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler) {
        return new TcpServer(binding, handler, vertx, tcpServerProperties);
    }

    static class TcpServer implements DeviceNetworkServer {
        private static final byte FRAME_START = 0x68;
        private static final byte FRAME_END = 0x16;
        private static final int ADDRESS_LENGTH = 7;
        private static final int MIN_FRAME_LENGTH = 12;
        private final DeviceGatewayBinding binding;
        private final DeviceNetworkHandler handler;
        private final Vertx vertx;
        private final TcpServerProperties tcpServerProperties;
        private final Map<String, NetSocket> deviceSockets = new ConcurrentHashMap<>();
        private final Map<String, Buffer> sessionBuffers = new ConcurrentHashMap<>();
        private volatile boolean running;
        private NetServer server;


        TcpServer(DeviceGatewayBinding binding,
                  DeviceNetworkHandler handler,
                  Vertx vertx,
                  TcpServerProperties tcpServerProperties) {
            this.binding = binding;
            this.handler = handler;
            this.vertx = vertx;
            this.tcpServerProperties = tcpServerProperties;
        }

        @Override public String bindingId() { return binding.getBindingId(); }
        @Override public String nodeId() { return binding.getNodeId(); }
        @Override public DeviceNetworkProtocol protocol() { return binding.getProtocol(); }

        @Override
        public void start() throws Exception {
            NetServerOptions options = new NetServerOptions()
                    .setHost(binding.getHost())
                    .setPort(binding.getPort())
                    .setTcpKeepAlive(true);
            if (tcpServerProperties.getIdleTimeoutSeconds() > 0) {
                options.setIdleTimeout(tcpServerProperties.getIdleTimeoutSeconds());
            }
            server = vertx.createNetServer(options);
            CompletableFuture<Void> future = new CompletableFuture<>();
            server.connectHandler(socket -> {
                final String sessionId = UUID.randomUUID().toString().replace("-", "");
                sessionBuffers.put(sessionId, Buffer.buffer());
                socket.handler(buffer -> handleBuffer(socket, buffer, sessionId));
                socket.closeHandler(v -> {
                    deviceSockets.values().removeIf(item -> item == socket);
                    sessionBuffers.remove(sessionId);
                });
            }).listen(binding.getPort(), binding.getHost(), ar -> {
                if (ar.succeeded()) future.complete(null);
                else future.completeExceptionally(ar.cause());
            });
            future.get();
            running = true;
        }

        private void handleBuffer(NetSocket socket, Buffer buffer, String sessionId) {
            if (looksLikeTextPayload(buffer)) {
                handleTextBuffer(socket, buffer, sessionId);
                return;
            }
            handleBinaryBuffer(socket, buffer, sessionId);
        }

        private void handleTextBuffer(NetSocket socket, Buffer buffer, String sessionId) {
            String payload = buffer.toString(StandardCharsets.UTF_8.name());
            for (String line : payload.split("\\r?\\n")) {
                publishTextMessage(socket, line, sessionId);
            }
        }

        private void publishTextMessage(NetSocket socket, String line, String sessionId) {
            String resolved = Strings.sNull(line).trim();
            if (Strings.isBlank(resolved)) {
                return;
            }
            Map<String, String> parsed = JsonPayloadSupport.extract(resolved);
            String deviceCode = Strings.sBlank(parsed.get("deviceCode"), sessionId);
            publishMessage(socket, sessionId, resolved, deviceCode, parsed);
        }

        private void handleBinaryBuffer(NetSocket socket, Buffer buffer, String sessionId) {
            Buffer pending = sessionBuffers.computeIfAbsent(sessionId, key -> Buffer.buffer());
            pending.appendBuffer(buffer);
            while (pending.length() >= MIN_FRAME_LENGTH) {
                int startIndex = findFrameStart(pending);
                if (startIndex < 0) {
                    sessionBuffers.put(sessionId, Buffer.buffer());
                    return;
                }
                if (startIndex > 0) {
                    pending = pending.slice(startIndex, pending.length());
                    sessionBuffers.put(sessionId, pending);
                }
                if (pending.length() < MIN_FRAME_LENGTH) {
                    return;
                }
                int dataLength = pending.getUnsignedByte(1 + ADDRESS_LENGTH + 1);
                int frameLength = MIN_FRAME_LENGTH + dataLength;
                if (pending.length() < frameLength) {
                    return;
                }
                if (pending.getByte(frameLength - 1) != FRAME_END) {
                    pending = pending.slice(1, pending.length());
                    sessionBuffers.put(sessionId, pending);
                    continue;
                }
                Buffer frame = pending.slice(0, frameLength);
                pending = pending.length() == frameLength ? Buffer.buffer() : pending.slice(frameLength, pending.length());
                sessionBuffers.put(sessionId, pending);
                publishBinaryMessage(socket, frame, sessionId);
            }
        }

        private void publishBinaryMessage(NetSocket socket, Buffer frame, String sessionId) {
            String payloadHex = bytesToHex(frame.getBytes());
            String deviceCode = resolveDeviceCode(frame);
            Map<String, String> headers = new HashMap<>();
            headers.put("payloadType", "HEX");
            headers.put("frameLength", String.valueOf(frame.length()));
            publishMessage(socket, sessionId, payloadHex, deviceCode, headers);
        }

        private void publishMessage(NetSocket socket, String sessionId, String payload, String deviceCode, Map<String, String> headers) {
            String resolvedDeviceCode = Strings.sBlank(deviceCode, sessionId).trim();
            deviceSockets.put(resolvedDeviceCode, socket);
            DeviceNetworkInboundMessage message = new DeviceNetworkInboundMessage();
            message.setBindingId(bindingId());
            message.setNodeId(nodeId());
            message.setProtocol(binding.getProtocol());
            message.setTenantId(Strings.sBlank(headers.get("tenantId"), binding.getTenantId()));
            message.setProductKey(Strings.sBlank(headers.get("productKey"), binding.getProductKey()));
            message.setDeviceCode(resolvedDeviceCode);
            message.setSessionId(sessionId);
            message.setRemoteAddress(socket.remoteAddress() != null ? socket.remoteAddress().toString() : "");
            message.setEndpoint(binding.getHost() + ":" + binding.getPort());
            message.setPayload(payload);
            message.setOccurredAt(System.currentTimeMillis());
            message.getHeaders().putAll(headers);
            message.getHeaders().put("protocolId", Strings.sNull(binding.getProtocolId()).trim());
            handler.onMessage(message);
        }

        private int findFrameStart(Buffer pending) {
            for (int index = 0; index < pending.length(); index++) {
                if (pending.getByte(index) == FRAME_START) {
                    return index;
                }
            }
            return -1;
        }

        private String resolveDeviceCode(Buffer frame) {
            if (frame.length() < MIN_FRAME_LENGTH || frame.getByte(0) != FRAME_START) {
                return "";
            }
            byte[] address = new byte[ADDRESS_LENGTH];
            for (int i = 0; i < ADDRESS_LENGTH; i++) {
                address[i] = frame.getByte(ADDRESS_LENGTH - i);
            }
            return bytesToHex(address);
        }

        private boolean looksLikeTextPayload(Buffer buffer) {
            for (byte value : buffer.getBytes()) {
                int current = value & 0xFF;
                if (current == '\n' || current == '\r' || current == '\t') {
                    continue;
                }
                if (current < 0x20 || current > 0x7E) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void stop() {
            running = false;
            if (server != null) server.close();
            deviceSockets.values().forEach(NetSocket::close);
            deviceSockets.clear();
            sessionBuffers.clear();
        }

        @Override public boolean isRunning() { return running; }

        @Override
        public void send(DeviceNetworkDownlinkMessage message) {
            NetSocket socket = deviceSockets.get(message.getDeviceCode());
            if (socket == null) {
                return;
            }
            String payload = Strings.sBlank(message.getPayload(), "");
            if (isHexPayload(payload)) {
                socket.write(Buffer.buffer(hexToBytes(payload)));
                return;
            }
            socket.write(Buffer.buffer(payload + "\n", StandardCharsets.UTF_8.name()));
        }

        private boolean isHexPayload(String payload) {
            String resolved = Strings.sNull(payload).replaceAll("\\s+", "");
            return Strings.isNotBlank(resolved) && resolved.length() % 2 == 0 && resolved.matches("[0-9A-Fa-f]+");
        }

        private byte[] hexToBytes(String hex) {
            String resolved = Strings.sNull(hex).replaceAll("\\s+", "").trim();
            byte[] bytes = new byte[resolved.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte) Integer.parseInt(resolved.substring(index, index + 2), 16);
            }
            return bytes;
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02X", value & 0xFF));
            }
            return builder.toString();
        }
    }
}
