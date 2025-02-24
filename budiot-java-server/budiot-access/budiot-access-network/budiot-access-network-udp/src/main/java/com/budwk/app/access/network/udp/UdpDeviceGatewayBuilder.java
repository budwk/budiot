package com.budwk.app.access.network.udp;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.message.impl.MessageTransferServer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(name = "udpGatewayBuilder", create = "init")
public class UdpDeviceGatewayBuilder implements DeviceGatewayBuilder {
    @Inject
    private MessageTransferServer messageTransferServer;
    private MessageTransfer messageTransfer;

    public void init() {
        messageTransfer = messageTransferServer.getMessageTransfer();
    }

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
