package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.starter.database.service.BaseService;

/**
 * @author wizzer
 */
public interface IotProductFirmwareService extends BaseService<Iot_product_firmware> {

    void deleteFirmware(String id);

    void deleteFirmwareDeviceId(String deviceId);
}