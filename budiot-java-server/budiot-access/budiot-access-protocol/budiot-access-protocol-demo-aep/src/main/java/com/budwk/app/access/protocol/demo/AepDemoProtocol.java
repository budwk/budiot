package com.budwk.app.access.protocol.demo;

import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.MessageCodec;
import com.budwk.app.access.protocol.codec.Protocol;
import com.budwk.app.access.protocol.codec.context.DeviceEventContext;
import com.budwk.app.access.protocol.demo.codec.HttpMessageCodec;
import com.budwk.app.access.protocol.device.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;

import java.util.Collections;
import java.util.List;

@Slf4j
public class AepDemoProtocol implements Protocol {

    public static final String PROTOCOL_CODE = "AEP_DEMO";
    public static final String PROTOCOL_NAME = "物联网表DEMO";

    @Override
    public String getCode() {
        return PROTOCOL_CODE;
    }

    @Override
    public String getName() {
        return PROTOCOL_NAME;
    }

    @Override
    public List<TransportType> getSupportedTransportTypes() {
        return Collections.singletonList(TransportType.HTTP);
    }

    @Override
    public MessageCodec getMessageCodec(TransportType transportType) {
        switch (transportType) {
            case HTTP:
                return new HttpMessageCodec();
        }
        return null;
    }

    @Override
    public void onDeviceRegistered(DeviceEventContext context, DeviceOperator deviceOperator) {
        log.info("AEP 设备注册开始");
        ProductInfo product = deviceOperator.getProduct();
        if (null == product || !TransportType.HTTP.name().equalsIgnoreCase(product.getProtocolType())) {
            return;
        }
        log.info("AEP 设备注册product {}", product);
        // 当前只有 AEP，其他的先忽略
        if ("AEP".equalsIgnoreCase(product.getIotPlatform())) {
            AepPlatformHelper aepPlatformHelper = new AepPlatformHelper(product.getProperties());
            String iotDevId = aepPlatformHelper.registerDevice(deviceOperator);
            if (Strings.isNotBlank(iotDevId)) {
                // 注册时绑定AEP平台的设备ID
                context.updateDeviceIotPlatformId(deviceOperator.getDeviceId(), iotDevId);
            }
        }
    }

    @Override
    public void onDeviceUnRegistered(DeviceEventContext context, DeviceOperator deviceOperator) {
        ProductInfo product = deviceOperator.getProduct();
        if (null == product || !TransportType.HTTP.name().equalsIgnoreCase(product.getProtocolType())) {
            return;
        }
        String iotDevId = Strings.sBlank(deviceOperator.getProperty("iotPlatformId"));
        // 当前只有 AEP，其他的先忽略
        if ("AEP".equalsIgnoreCase(product.getIotPlatform())) {
            AepPlatformHelper aepPlatformHelper = new AepPlatformHelper(product.getProperties());
            aepPlatformHelper.deleteDevice(iotDevId);
        }
    }
}
