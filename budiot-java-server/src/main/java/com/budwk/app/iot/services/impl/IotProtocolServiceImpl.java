package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_protocol;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.app.iot.services.IotProtocolService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProtocolServiceImpl extends BaseServiceImpl<Iot_protocol> implements IotProtocolService {
    public IotProtocolServiceImpl(Dao dao) {
        super(dao);
    }
}
