package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_attr;
import com.budwk.app.iot.services.IotProductAttrService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductAttrServiceImpl extends BaseServiceImpl<Iot_product_attr> implements IotProductAttrService {
    public IotProductAttrServiceImpl(Dao dao) {
        super(dao);
    }
}
