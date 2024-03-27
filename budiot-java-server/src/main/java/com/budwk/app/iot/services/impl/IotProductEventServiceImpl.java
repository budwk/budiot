package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_event;
import com.budwk.app.iot.services.IotProductEventService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductEventServiceImpl extends BaseServiceImpl<Iot_product_event> implements IotProductEventService {
    public IotProductEventServiceImpl(Dao dao) {
        super(dao);
    }
}
