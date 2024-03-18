package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotDeviceServiceImpl extends BaseServiceImpl<Iot_device> implements IotDeviceService {
    public IotDeviceServiceImpl(Dao dao) {
        super(dao);
    }
}
