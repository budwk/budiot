package com.budwk.app.access.network.udp.server;


import com.budwk.app.access.network.udp.client.UdpSender;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

public interface UdpServer {
    UdpServer start();

    CompletionStage<Void> send(UdpSender sender, byte[] message);

    UdpServer onMessage(BiConsumer<UdpSender, byte[]> messageHandler);
}
