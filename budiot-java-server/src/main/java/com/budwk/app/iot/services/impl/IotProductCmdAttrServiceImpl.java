package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_cmd_attr;
import com.budwk.app.iot.services.IotProductCmdAttrService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotProductCmdAttrServiceImpl extends BaseServiceImpl<Iot_product_cmd_attr> implements IotProductCmdAttrService {
    public IotProductCmdAttrServiceImpl(Dao dao) {
        super(dao);
    }
}
