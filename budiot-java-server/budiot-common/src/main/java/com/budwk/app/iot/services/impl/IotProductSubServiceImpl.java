package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.enums.SubscribeType;
import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.services.IotProductSubService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.List;

@IocBean(args = {"refer:dao"})
public class IotProductSubServiceImpl extends BaseServiceImpl<Iot_product_sub> implements IotProductSubService {
    public IotProductSubServiceImpl(Dao dao) {
        super(dao);
    }

    public List<Iot_product_sub> getList(String productId, SubscribeType type) {
        return this.query(Cnd.where("productId", "=", productId).and("subType", "=", type.value()));
    }
}
