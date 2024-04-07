package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.app.iot.services.IotProductSubService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductSubServiceImpl extends BaseServiceImpl<Iot_product_sub> implements IotProductSubService {
    public IotProductSubServiceImpl(Dao dao) {
        super(dao);
    }
}
