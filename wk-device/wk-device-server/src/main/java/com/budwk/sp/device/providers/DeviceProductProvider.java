package com.budwk.sp.device.providers;

import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.services.DeviceProductService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = IDeviceProductProvider.class)
public class DeviceProductProvider implements IDeviceProductProvider {
    private final DeviceProductService deviceProductService;

    public DeviceProductProvider(DeviceProductService deviceProductService) {
        this.deviceProductService = deviceProductService;
    }

    @Override
    public Device_product getById(String id, String tenantId) { return deviceProductService.getProduct(id, tenantId); }

    @Override
    public Device_product getByProductKey(String productKey, String tenantId) { return deviceProductService.getByProductKey(productKey, tenantId); }
}
