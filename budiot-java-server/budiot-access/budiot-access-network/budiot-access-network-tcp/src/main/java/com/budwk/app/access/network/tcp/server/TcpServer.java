package com.budwk.app.access.network.tcp.server;


import com.budwk.app.access.network.tcp.client.TcpClient;

import java.util.function.Consumer;

public interface TcpServer {
    TcpServer start();

    TcpServer handleConnection(Consumer<TcpClient> handler);

}
