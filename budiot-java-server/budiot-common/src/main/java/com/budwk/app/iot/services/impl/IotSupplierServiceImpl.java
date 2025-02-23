package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_supplier;
import com.budwk.app.iot.services.IotSupplierService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotSupplierServiceImpl extends BaseServiceImpl<Iot_supplier> implements IotSupplierService {
    public IotSupplierServiceImpl(Dao dao) {
        super(dao);
    }
}
