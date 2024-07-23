package com.budwk.app.network.modbus;

import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * modbusSlaveGatewayBuilder = 名称规则：协议+GatewayBuilder
 */
@IocBean(name = "modbusSlaveGatewayBuilder")
public class ModbusSlaveDeviceGatewayBuilder implements DeviceGatewayBuilder {

    @Inject
    private MessageTransfer messageTransfer;

    @Override
    public String getId() {
        return "modbus-slave";
    }

    @Override
    public String getName() {
        return "MODBUS-SLAVE";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MODBUS;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new ModbusSlaveDeviceGateway(configuration, messageTransfer);
    }
}
