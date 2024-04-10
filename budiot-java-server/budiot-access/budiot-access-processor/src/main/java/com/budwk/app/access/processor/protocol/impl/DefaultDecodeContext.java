package com.budwk.app.access.processor.protocol.impl;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.DeviceRegistry;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import org.nutz.lang.Strings;

public abstract class DefaultDecodeContext implements DecodeContext {

    private final DeviceRegistry deviceRegistry;

    private final EncodedMessage message;
    private DeviceOperator deviceOperator;

    public DefaultDecodeContext(DeviceRegistry deviceRegistry, EncodedMessage message) {
        this.deviceRegistry = deviceRegistry;
        this.message = message;
    }

    @Override
    public DeviceOperator getGatewayDevice(String field, String value) {
        if (this.deviceOperator != null && Strings.sBlank(deviceOperator.getProperty(field)).equals(value)) {
            return deviceOperator;
        }
        return deviceRegistry.getGatewayDevice(field, value);
    }

    @Override
    public EncodedMessage getMessage() {
        return this.message;
    }

    @Override
    public DeviceOperator getDevice(String meterNo) {
        return this.getDevice("meterNo", meterNo);
    }

    @Override
    public DeviceOperator getDevice(String field, String value) {
        if (null != deviceOperator && Strings.sBlank(deviceOperator.getProperty(field)).equals(value)) {
            return deviceOperator;
        }
        this.deviceOperator = deviceRegistry.getDeviceOperator(field, value);
        return this.deviceOperator;
    }

    @Override
    public DeviceOperator getDevice() {
        return this.deviceOperator;
    }

    @Override
    public abstract void send(EncodedMessage replyMessage);

    public void setDevice(DeviceOperator deviceOperator) {
        this.deviceOperator = deviceOperator;
    }

}
