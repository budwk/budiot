package com.budwk.app.access.network.tcp.server;

import com.budwk.app.access.network.tcp.client.TcpClient;
import com.budwk.app.access.network.tcp.client.VertxTcpClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.util.NutMap;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class VertxTcpServer implements TcpServer {

    private NutMap configs;
    private Consumer<TcpClient> connectHandler;

    private static final Vertx vertx = Vertx.vertx();

    public VertxTcpServer(Map<String, Object> configs) {
        this.configs = NutMap.WRAP(configs);
    }

    public TcpServer start() {
        NetServerOptions options = new NetServerOptions(JsonObject.mapFrom(configs));
        options.setHost(configs.getString("host", "0.0.0.0"));
        options.setPort(configs.getInt("port", 0));
        options.setSsl(configs.getBoolean("ssl", false));

        vertx.createNetServer(options)
                .connectHandler(this::onConnect)
                .listen()
                .onSuccess(netServer -> log.debug("tcp server started at {}", netServer.actualPort()))
                .onFailure(throwable -> log.error("tcp server error", throwable));
        return this;
    }

    @Override
    public TcpServer handleConnection(Consumer<TcpClient> handler) {
        this.connectHandler = handler;
        return this;
    }

    private void onConnect(NetSocket netSocket) {
        TcpClient client = new VertxTcpClient(netSocket);
        if (this.connectHandler != null) {
            this.connectHandler.accept(client);
        }
    }

}
