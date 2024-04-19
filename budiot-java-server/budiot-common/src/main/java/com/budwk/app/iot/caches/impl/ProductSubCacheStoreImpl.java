package com.budwk.app.iot.caches.impl;

import com.budwk.app.iot.caches.ProductSubCacheStore;
import com.budwk.app.iot.models.Iot_product_sub;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.List;

@IocBean(create = "init")
public class ProductSubCacheStoreImpl implements ProductSubCacheStore {
    private final static String cacheName = "PRODUCT_SUB_CACHE";
    @Inject
    private RedissonClient redissonClient;
    RMapCache<String, Object> rMapCache;

    public void init() {
        rMapCache = redissonClient.getMapCache(cacheName);
    }

    public List<Iot_product_sub> cache(String productId, List<Iot_product_sub> list) {
        rMapCache.put(productId, list);
        return list;
    }

    public List<Iot_product_sub> getSubCache(String productId) {
        Object obj = rMapCache.get(productId);
        if (obj != null) {
            return (List<Iot_product_sub>) rMapCache.get(productId);
        }
        return null;
    }

}
