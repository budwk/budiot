package com.budwk.app.access.processor.protocol.impl;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.DeviceRegistry;
import com.budwk.app.access.protocol.codec.impl.DefaultDeviceOperator;
import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.device.ProductInfo;
import com.budwk.app.access.storage.DeviceCmdDataStorage;
import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_device_cmd;
import com.budwk.app.iot.models.Iot_device_prop;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.services.IotDeviceCmdService;
import com.budwk.app.iot.services.IotDevicePropService;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean
public class DefaultDeviceRegistry implements DeviceRegistry {
    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private IotDevicePropService iotDevicePropService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotDeviceCmdService iotDeviceCmdService;

    @Override
    public DeviceOperator getGatewayDevice(String protocolCode, String deviceNo) {
        Iot_device device = iotDeviceService.fetch(Cnd.where("protocolCode", "=", protocolCode).and("deviceNo", "=", deviceNo));
        if (device == null) {
            return null;
        }
        Iot_product product = iotProductService.fetch(device.getProductId());
        // 设置设备缓存
        iotDeviceService.setCache(device,product);
        return this.buildDefaultOperator(device, product);
    }

    @Override
    public DeviceOperator getDeviceOperator(String protocolCode, String deviceNo) {
        Iot_device device = iotDeviceService.fetch(Cnd.where("protocolCode", "=", protocolCode).and("deviceNo", "=", deviceNo));
        if (device == null) {
            return null;
        }
        Iot_product product = iotProductService.fetch(device.getProductId());
        // 设置设备缓存
        iotDeviceService.setCache(device,product);
        return this.buildDefaultOperator(device, product);
    }

    @Override
    public DeviceOperator getDeviceOperator(String deviceId) {
        Iot_device device = iotDeviceService.fetch(Cnd.where("id", "=", deviceId));
        if (device == null) {
            return null;
        }
        Iot_product product = iotProductService.fetch(device.getProductId());
        // 设置设备缓存
        iotDeviceService.setCache(device,product);
        return this.buildDefaultOperator(device, product);
    }

    @Override
    public CommandInfo getDeviceCommand(String deviceId) {
        Iot_device_cmd needSendCommand = iotDeviceCmdService.fetch(Cnd.where("deviceId", "=", deviceId).and("status", "=", DeviceCmdStatus.WAIT.value()).asc("id"));
        if (null != needSendCommand) {
            CommandInfo commandInfo = new CommandInfo();
            commandInfo.setCommandId(needSendCommand.getId());
            commandInfo.setCommandSerialNo(needSendCommand.getSerialNo());
            commandInfo.setDeviceId(deviceId);
            commandInfo.setCommandCode(needSendCommand.getCode());
            commandInfo.setParams(needSendCommand.getParams());
            return commandInfo;
        }
        return null;
    }

    public ProductInfo getProductInfo(String productId) {
        Iot_product product = iotProductService.fetch(productId);
        if (product == null) {
            return null;
        }
        return buildProductInfo(product);
    }

    @Override
    public void updateDeviceOnline(String deviceId) {
        iotDeviceService.doUpdateOnline(deviceId);
    }

    @Override
    public void makeCommandSend(String cmdId) {
        iotDeviceCmdService.makeCommandSend(cmdId);
    }

    @Override
    public void makeCommandFinish(String cmdId, int status, String result) {
        iotDeviceCmdService.makeCommandFinish(cmdId, DeviceCmdStatus.from(status), result);
    }

    private DeviceOperator buildDefaultOperator(Iot_device device, Iot_product product) {
        DefaultDeviceOperator operator = new DefaultDeviceOperator() {
            @Override
            public int getWaitCmdCount() {
                return iotDeviceCmdService.getWaitCount(getDeviceId());
            }
        };
        operator.setDeviceId(device.getId());
        fillOperator(device, operator);
        operator.setProductInfo(buildProductInfo(product));
        return operator;
    }

    private void fillOperator(Iot_device device, DefaultDeviceOperator operator) {
        operator.setDeviceId(device.getId());
        operator.setProperty("meterNo", device.getMeterNo());
        operator.setProperty("deviceNo", device.getDeviceNo());
        operator.setProperty("protocolCode", device.getProtocolCode());
        operator.setProperty("imei", device.getImei());
        operator.setProperty("iccid", device.getIccid());
        operator.setProperty("iotPlatformId", device.getIotPlatformId());
        operator.setProperty("lastConnectionTime", device.getLastConnectionTime());
        operator.setProperty("lastDataTime", device.getLastDataTime());
        Iot_device_prop deviceProp = iotDevicePropService.fetch(Cnd.where("deviceId", "=", device.getId()));
        if (null != deviceProp && null != deviceProp.getProperties()) {
            operator.getProperties().putAll(deviceProp.getProperties());
        }
        operator.getUpdatedProperties().clear();
    }

    private ProductInfo buildProductInfo(Iot_product product) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(product.getId());
        productInfo.setClassifyId(product.getClassifyId());
        productInfo.setName(product.getName());
        productInfo.setDataFormat(product.getDataFormat());
        productInfo.setIotPlatform(product.getIotPlatform().name());
        productInfo.setProtocolType(product.getProtocolType().name());
        productInfo.setPayMode(product.getPayMode());
        productInfo.setProperties(product.getProperties());
        return productInfo;
    }
}
