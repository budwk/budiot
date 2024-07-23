package com.budwk.app.access.network.modbus;

import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * modbusMasterGatewayBuilder = 名称规则：协议+GatewayBuilder
 */
@IocBean(name = "modbusMasterGatewayBuilder")
public class ModbusMasterDeviceGatewayBuilder implements DeviceGatewayBuilder {

    @Inject
    private MessageTransfer messageTransfer;

    @Override
    public String getId() {
        return "modbus-master";
    }

    @Override
    public String getName() {
        return "MODBUS-MASTER";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MODBUS;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new ModbusMasterDeviceGateway(configuration, messageTransfer);
    }
}
