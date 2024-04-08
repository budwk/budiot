package com.budwk.app.access.network.udp.server;

import com.budwk.app.access.network.udp.client.UdpSender;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.util.NutMap;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@Slf4j
public class VertxUdpServer implements UdpServer {

    private NutMap configs;

    private BiConsumer<UdpSender, byte[]> messageHandler;
    private static final Vertx vertx = Vertx.vertx();

    public VertxUdpServer(Map<String, Object> configs) {
        this.configs = NutMap.WRAP(configs);
    }

    private DatagramSocket datagramSocket;

    @Override
    public UdpServer start() {
        if (null != datagramSocket) {
            return this;
        }
        DatagramSocketOptions options = new DatagramSocketOptions();
        String host = configs.getString("host", "0.0.0.0");
        if (host.startsWith("[") && host.endsWith("]")) {
            options.setIpV6(true);
        }
        int port = configs.getInt("port", 0);
        datagramSocket = vertx.createDatagramSocket(options);
        datagramSocket.handler(packet -> {
            if (null != messageHandler) {
                SocketAddress sender = packet.sender();
                messageHandler.accept(new UdpSender(sender.host(), sender.port()), packet.data().getBytes());
            }
        });
        datagramSocket.listen(port, host)
                .onSuccess(event -> {
                    log.info("udp server started at {}", configs.getInt("port", 0));

                }).onFailure(throwable -> {
                    log.error("udp server error", throwable);
                });
        return this;
    }

    @Override
    public CompletionStage<Void> send(UdpSender sender, byte[] message) {
        return datagramSocket.send(Buffer.buffer(message), sender.getPort(), sender.getHost()).toCompletionStage();
    }

    @Override
    public UdpServer onMessage(BiConsumer<UdpSender, byte[]> messageHandler) {
        this.messageHandler = messageHandler;
        return this;
    }

}
