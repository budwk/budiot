package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.starter.database.service.BaseService;

import java.util.List;

public interface IotDeviceService extends BaseService<Iot_device> {
    void importData(String fileName, List<Iot_device> list, boolean over, String userId, String loginname);
}
