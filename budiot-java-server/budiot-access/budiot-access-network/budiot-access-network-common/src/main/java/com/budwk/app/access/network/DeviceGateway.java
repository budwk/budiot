package com.budwk.app.access.network;

public interface DeviceGateway {
    /**
     * 获取传输协议
     * @return
     */
    TransportType getTransportType();

    /**
     * 启动
     */
    void start();
}
