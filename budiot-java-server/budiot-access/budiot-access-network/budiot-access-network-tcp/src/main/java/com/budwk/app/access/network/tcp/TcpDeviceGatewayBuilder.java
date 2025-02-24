package com.budwk.app.access.network.tcp;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.message.impl.MessageTransferServer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(name = "tcpGatewayBuilder", create = "init")
public class TcpDeviceGatewayBuilder implements DeviceGatewayBuilder {
    @Inject
    private MessageTransferServer messageTransferServer;
    private MessageTransfer messageTransfer;

    public void init() {
        messageTransfer = messageTransferServer.getMessageTransfer();
    }

    @Override
    public String getId() {
        return "tcp";
    }

    @Override
    public String getName() {
        return "TCP";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TCP;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new TcpDeviceGateway(configuration, messageTransfer);
    }
}
