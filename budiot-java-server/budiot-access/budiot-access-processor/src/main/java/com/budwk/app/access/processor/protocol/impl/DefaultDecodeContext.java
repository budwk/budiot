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
    private String protocolCode;

    public DefaultDecodeContext(String protocolCode, DeviceRegistry deviceRegistry, EncodedMessage message) {
        this.protocolCode = protocolCode;
        this.deviceRegistry = deviceRegistry;
        this.message = message;
    }

    @Override
    public DeviceOperator getGatewayDeviceByNo(String deviceNo) {
        if (this.deviceOperator != null && Strings.sBlank(deviceOperator.getProperty("protocolCode")).equals(protocolCode) && Strings.sBlank(deviceOperator.getProperty("deviceNo")).equals(deviceNo)) {
            return deviceOperator;
        }
        return deviceRegistry.getGatewayDevice(protocolCode, deviceNo);
    }

    @Override
    public DeviceOperator getGatewayDeviceById(String deviceId) {
        if (this.deviceOperator != null && deviceOperator.getDeviceId().equals(deviceId)) {
            return deviceOperator;
        }
        return deviceRegistry.getGatewayDevice(protocolCode, deviceId);
    }

    @Override
    public EncodedMessage getMessage() {
        return this.message;
    }

    /**
     * 通过设备通信号获取对象
     *
     * @param deviceNo 设备通信号
     * @return
     */
    @Override
    public DeviceOperator getDeviceByNo(String deviceNo) {
        if (null != deviceOperator && Strings.sBlank(deviceOperator.getProperty("protocolCode")).equals(protocolCode) && Strings.sBlank(deviceOperator.getProperty("deviceNo")).equals(deviceNo)) {
            return deviceOperator;
        }
        this.deviceOperator = deviceRegistry.getDeviceOperator(protocolCode, deviceNo);
        return this.deviceOperator;
    }

    /**
     * 通过设备imei获取对象
     *
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     * @return
     */
    @Override
    public DeviceOperator getDeviceByField(String fieldName, String fieldValue) {
        if (null != deviceOperator && Strings.sBlank(deviceOperator.getProperty("protocolCode")).equals(protocolCode) && Strings.sBlank(deviceOperator.getProperty(fieldName)).equals(fieldValue)) {
            return deviceOperator;
        }
        this.deviceOperator = deviceRegistry.getDeviceOperator(protocolCode, fieldName, fieldValue);
        return this.deviceOperator;
    }

    /**
     * 通过设备ID获取对象
     *
     * @param deviceId 设备ID
     * @return
     */
    @Override
    public DeviceOperator getDeviceById(String deviceId) {
        if (null != deviceOperator && deviceOperator.getDeviceId().equals(deviceId)) {
            return deviceOperator;
        }
        this.deviceOperator = deviceRegistry.getDeviceOperator(deviceId);
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
