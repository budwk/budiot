package com.budwk.app.access.network.tcp.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class VertxTcpClient implements TcpClient {

    private String id;
    private NetSocket socket;

    public VertxTcpClient(NetSocket socket) {
        this.id = socket.remoteAddress().toString();
        this.socket = socket;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TcpClient onMessage(Consumer<byte[]> messageHandler) {
        socket.handler(buffer -> messageHandler.accept(buffer.getBytes()));
        return this;
    }

    @Override
    public TcpClient onClose(Consumer<Void> closeHandler) {
        socket.closeHandler(closeHandler::accept);
        return this;
    }

    @Override
    public TcpClient onError(Consumer<Throwable> errorHandler) {
        socket.exceptionHandler(errorHandler::accept);
        return this;
    }

    @Override
    public CompletionStage<Void> send(byte[] bytes) {
        return socket.write(Buffer.buffer(bytes)).toCompletionStage();
    }
}
