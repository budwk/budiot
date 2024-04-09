package com.budwk.app.access.network.http;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * HTTP 网关构造器
 */
@IocBean(name = "httpGatewayBuilder")
public class HttpDeviceGatewayBuilder implements DeviceGatewayBuilder {
    @Inject
    private MessageTransfer messageTransfer;

    @Override
    public String getId() {
        return "http";
    }

    @Override
    public String getName() {
        return "HTTP";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.HTTP;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new HttpDeviceGateway(configuration, messageTransfer);
    }
}
