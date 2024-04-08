package com.budwk.app.access.network.udp.client;

import lombok.Data;

@Data
public class UdpSender {
    private final String id;
    private final String host;
    private final int port;

    public UdpSender(String host, int port) {
        this.host = host;
        this.port = port;
        this.id = host + ":" + port;
    }
}
