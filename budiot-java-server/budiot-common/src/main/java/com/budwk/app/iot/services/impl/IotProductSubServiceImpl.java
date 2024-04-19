package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.caches.ProductSubCacheStore;
import com.budwk.app.iot.enums.SubscribeType;
import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.services.IotProductSubService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.List;

@IocBean(args = {"refer:dao"})
public class IotProductSubServiceImpl extends BaseServiceImpl<Iot_product_sub> implements IotProductSubService {
    public IotProductSubServiceImpl(Dao dao) {
        super(dao);
    }

    @Inject
    private ProductSubCacheStore productSubCacheStore;

    public List<Iot_product_sub> getList(String productId, SubscribeType type) {
        return this.query(Cnd.where("productId", "=", productId).and("enabled", "=", true).and("subType", "=", type.value()));
    }

    public List<Iot_product_sub> setCache(String productId) {
        return productSubCacheStore.cache(productId, this.query(Cnd.where("productId", "=", productId).and("enabled", "=", true)));
    }

    public List<Iot_product_sub> getSubCache(String productId) {
        List<Iot_product_sub> list = productSubCacheStore.getSubCache(productId);
        if (list == null) {
            return this.setCache(productId);
        }
        return list;
    }
}
