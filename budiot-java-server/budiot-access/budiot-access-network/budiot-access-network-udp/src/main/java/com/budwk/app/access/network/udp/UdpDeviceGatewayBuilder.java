package com.budwk.app.access.network.udp;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.network.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(name = "udpGatewayBuilder")
public class UdpDeviceGatewayBuilder implements DeviceGatewayBuilder {

    @Inject
    private MessageTransfer messageTransfer;

    @Override
    public String getId() {
        return "udp";
    }

    @Override
    public String getName() {
        return "UDP";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.UDP;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new UdpDeviceGateway(configuration, messageTransfer);
    }
}
