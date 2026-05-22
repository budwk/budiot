package com.budwk.sp.device.network;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;

public interface DeviceNetworkServer {
    String bindingId();
    String nodeId();
    DeviceNetworkProtocol protocol();
    void start() throws Exception;
    void stop() throws Exception;
    boolean isRunning();
    void send(DeviceNetworkDownlinkMessage message);
}
