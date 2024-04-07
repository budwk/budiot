package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_prop;
import com.budwk.app.iot.services.IotProductPropService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductPropServiceImpl extends BaseServiceImpl<Iot_product_prop> implements IotProductPropService {
    public IotProductPropServiceImpl(Dao dao) {
        super(dao);
    }
}
