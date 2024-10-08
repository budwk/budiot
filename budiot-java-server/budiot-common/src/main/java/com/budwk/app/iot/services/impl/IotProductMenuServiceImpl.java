package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_product_menu;
import com.budwk.app.iot.services.IotProductMenuService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * @author wizzer
 */
@IocBean(args = {"refer:dao"})
public class IotProductMenuServiceImpl extends BaseServiceImpl<Iot_product_menu> implements IotProductMenuService {
    public IotProductMenuServiceImpl(Dao dao) {
        super(dao);
    }
}