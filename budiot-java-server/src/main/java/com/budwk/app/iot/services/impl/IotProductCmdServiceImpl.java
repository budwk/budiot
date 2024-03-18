package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.models.Iot_product_cmd;
import com.budwk.app.iot.services.IotProductCmdService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductCmdServiceImpl extends BaseServiceImpl<Iot_product_cmd> implements IotProductCmdService {
    public IotProductCmdServiceImpl(Dao dao) {
        super(dao);
    }
}
