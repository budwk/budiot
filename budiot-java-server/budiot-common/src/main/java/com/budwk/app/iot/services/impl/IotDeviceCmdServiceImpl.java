package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.app.iot.models.Iot_device_cmd;
import com.budwk.app.iot.services.IotDeviceCmdService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotDeviceCmdServiceImpl extends BaseServiceImpl<Iot_device_cmd> implements IotDeviceCmdService {
    public IotDeviceCmdServiceImpl(Dao dao) {
        super(dao);
    }

    public int getWaitCount(String deviceId) {
        return this.count(Cnd.where("deviceId", "=", deviceId).and("status", "=", DeviceCmdStatus.WAIT.value()));
    }
}
