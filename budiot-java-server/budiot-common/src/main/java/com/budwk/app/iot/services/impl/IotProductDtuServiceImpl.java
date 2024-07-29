package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_dtu;
import com.budwk.app.iot.services.IotProductDtuService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
public class IotProductDtuServiceImpl extends BaseServiceImpl<Iot_product_dtu> implements IotProductDtuService {
    public IotProductDtuServiceImpl(Dao dao) {
        super(dao);
    }
}