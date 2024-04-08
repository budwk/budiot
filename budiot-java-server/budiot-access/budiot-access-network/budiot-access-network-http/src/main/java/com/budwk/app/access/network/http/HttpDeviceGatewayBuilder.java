package com.budwk.app.access.network.http;

import com.osdiot.gas.device.networks.config.DeviceGatewayConfiguration;
import com.osdiot.gas.device.networks.core.DeviceGateway;
import com.osdiot.gas.device.networks.core.DeviceGatewayBuilder;
import com.osdiot.gas.device.protocols.dto.enums.TransportType;
import com.osdiot.gas.starter.message.MessageTransfer;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * HTTP 网关构造器
 *
 * @author zyang  2022/6/9 19:04
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
        return "http网关";
    }

    @Override
    public TransportType getTransportType() {
        return null;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new HttpDeviceGateway(configuration, messageTransfer);
    }
}
