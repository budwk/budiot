package com.budwk.sp.device.providers;

import com.budwk.sp.device.entity.Device_product;

public interface IDeviceProductProvider {
    Device_product getById(String id, String tenantId);
    Device_product getByProductKey(String productKey, String tenantId);
}
