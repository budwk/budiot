package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device_log;
import com.budwk.app.iot.services.IotDeviceLogService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotDeviceLogServiceImpl extends BaseServiceImpl<Iot_device_log> implements IotDeviceLogService {
    public IotDeviceLogServiceImpl(Dao dao) {
        super(dao);
    }
}
