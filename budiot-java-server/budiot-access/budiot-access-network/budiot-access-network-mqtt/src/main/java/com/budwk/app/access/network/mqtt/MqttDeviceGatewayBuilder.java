package com.budwk.app.access.network.mqtt;

import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.message.impl.MessageTransferServer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayBuilder;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * @author wizzer.cn
 */
@IocBean(name = "mqttGatewayBuilder", create = "init")
public class MqttDeviceGatewayBuilder implements DeviceGatewayBuilder {
    private static String name = "mqtt";
    @Inject
    private MessageTransferServer messageTransferServer;
    private MessageTransfer messageTransfer;

    public void init() {
        messageTransfer = messageTransferServer.getMessageTransfer();
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public String getName() {
        return "MQTT";
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
    }

    @Override
    public DeviceGateway buildGateway(DeviceGatewayConfiguration configuration) {
        return new MqttDeviceGateway(configuration, messageTransfer);
    }
}
