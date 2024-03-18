package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductServiceImpl extends BaseServiceImpl<Iot_product> implements IotProductService {
    public IotProductServiceImpl(Dao dao) {
        super(dao);
    }
}
