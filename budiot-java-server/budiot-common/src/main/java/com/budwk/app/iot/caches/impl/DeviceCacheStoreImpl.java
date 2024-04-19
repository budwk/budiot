package com.budwk.app.iot.caches.impl;

import com.budwk.app.iot.caches.DeviceCacheStore;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

@IocBean(create = "init")
public class DeviceCacheStoreImpl implements DeviceCacheStore {
    private final static String cacheName = "DEVICE";
    @Inject
    private RedissonClient redissonClient;
    RMapCache<String, Object> rMapCache;

    public void init() {
        rMapCache = redissonClient.getMapCache(cacheName);
    }

    public void cache(Iot_device device, Iot_product product) {
        DeviceProcessCache deviceProcessCache = new DeviceProcessCache();
        deviceProcessCache.setParentId(device.getParentId());
        deviceProcessCache.setDeviceId(device.getId());
        deviceProcessCache.setDeviceNo(device.getDeviceNo());
        deviceProcessCache.setMeterNo(device.getMeterNo());
        deviceProcessCache.setAbnormal(device.getAbnormal());
        deviceProcessCache.setAbnormalTime(device.getAbnormalTime());
        deviceProcessCache.setClassifyId(device.getClassifyId());
        deviceProcessCache.setValveState(device.getValveState().value());
        deviceProcessCache.setProtocolCode(device.getProtocolCode());
        deviceProcessCache.setProductId(device.getProductId());
        deviceProcessCache.setPayMode(product.getPayMode());
        deviceProcessCache.setLastDataTime(device.getLastDataTime());
        deviceProcessCache.setRefreshTime(System.currentTimeMillis());
        deviceProcessCache.setProductProperties(product.getProperties());
        deviceProcessCache.setDeviceType(product.getDeviceType());
        deviceProcessCache.setDataFormat(product.getDataFormat());
        rMapCache.put(device.getId(), deviceProcessCache);
    }

    public DeviceProcessCache getDevice(String deviceId) {
        return (DeviceProcessCache) rMapCache.get(deviceId);
    }

    public void update(DeviceProcessCache deviceProcessCache) {
        rMapCache.put(deviceProcessCache.getDeviceId(), deviceProcessCache);
    }
}
