package com.budwk.app.iot.caches;

import com.budwk.app.iot.models.Iot_product_sub;

import java.util.List;

public interface ProductSubCacheStore {
    List<Iot_product_sub> cache(String productId, List<Iot_product_sub> list);

    List<Iot_product_sub> getSubCache(String productId);

}
