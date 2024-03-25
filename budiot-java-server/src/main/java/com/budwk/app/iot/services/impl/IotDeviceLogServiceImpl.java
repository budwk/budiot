package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_device_log;
import com.budwk.app.iot.services.IotDeviceLogService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.aop.interceptor.async.Async;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotDeviceLogServiceImpl extends BaseServiceImpl<Iot_device_log> implements IotDeviceLogService {
    public IotDeviceLogServiceImpl(Dao dao) {
        super(dao);
    }

    @Async
    public void log(String note, String deviceId, String operatorId, String operatorName) {
        Iot_device_log log = new Iot_device_log();
        log.setDeviceId(deviceId);
        log.setNote(note);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperateTime(System.currentTimeMillis());
        this.insert(log);
    }
}
