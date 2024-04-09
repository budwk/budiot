package com.budwk.app.access.network;

import com.budwk.app.access.enums.TransportType;

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
