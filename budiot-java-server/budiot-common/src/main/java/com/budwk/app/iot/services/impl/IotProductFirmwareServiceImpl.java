package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.app.iot.models.Iot_product_firmware_device;
import com.budwk.app.iot.services.IotProductFirmwareService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.aop.interceptor.ioc.TransAop;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.aop.Aop;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
public class IotProductFirmwareServiceImpl extends BaseServiceImpl<Iot_product_firmware> implements IotProductFirmwareService {
    public IotProductFirmwareServiceImpl(Dao dao) {
        super(dao);
    }

    @Aop(TransAop.READ_COMMITTED)
    public void deleteFirmware(String id) {
        this.delete(id);
        this.dao().clear(Iot_product_firmware_device.class, Cnd.where("firmwareId", "=", id));
    }

    @Aop(TransAop.READ_COMMITTED)
    public void deleteFirmwareDeviceId(String deviceId) {
        this.dao().clear(Iot_product_firmware_device.class, Cnd.where("deviceId", "=", deviceId));
    }
}