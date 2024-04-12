package com.budwk.app.iot.services;

import com.budwk.app.iot.enums.DeviceCmdStatus;
import com.budwk.app.iot.models.Iot_device_cmd;
import com.budwk.starter.database.service.BaseService;

public interface IotDeviceCmdService extends BaseService<Iot_device_cmd> {
    int getWaitCount(String deviceId);

    void makeCommandSend(String cmdId);

    void makeCommandFinish(String cmdId, DeviceCmdStatus status, String result);
}
