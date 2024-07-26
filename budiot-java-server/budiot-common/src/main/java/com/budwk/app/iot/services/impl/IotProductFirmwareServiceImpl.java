package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.app.iot.services.IotProductFirmwareService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
public class IotProductFirmwareServiceImpl extends BaseServiceImpl<Iot_product_firmware> implements IotProductFirmwareService {
    public IotProductFirmwareServiceImpl(Dao dao) {
        super(dao);
    }
}