package com.budwk.app.iot.caches.impl;

import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.List;

@IocBean(create = "init")
public class ProductSubCacheStoreImpl {
    private final static String cacheName = "PRODUCT_SUB";
    @Inject
    private RedissonClient redissonClient;
    RMapCache<String, Object> rMapCache;

    public void init() {
        rMapCache = redissonClient.getMapCache(cacheName);
    }

    public List<Iot_product_sub> getSubCache(String productId){
        return (DeviceProcessCache) rMapCache.getAll(productId);
    }

    public  void updateSubCache(String productId){

    }
}
