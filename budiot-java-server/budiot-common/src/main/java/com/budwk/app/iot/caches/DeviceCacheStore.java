package com.budwk.app.iot.caches;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;

public interface DeviceCacheStore {
    void cache(Iot_device device, Iot_product product);
    DeviceProcessCache getDevice(String deviceId);

    void update(DeviceProcessCache deviceProcessCache);
}
