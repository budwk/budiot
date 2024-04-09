package com.budwk.app.access.network;

import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;

public interface DeviceGatewayBuilder {
    /**
     * 获取ID
     *
     * @return
     */
    String getId();

    /**
     * 获取名称
     *
     * @return
     */
    String getName();

    /**
     * 获取传输协议
     *
     * @return
     */
    TransportType getTransportType();

    /**
     * 构造网关
     *
     * @param configuration 配置参数
     * @return
     */
    DeviceGateway buildGateway(DeviceGatewayConfiguration configuration);
}
