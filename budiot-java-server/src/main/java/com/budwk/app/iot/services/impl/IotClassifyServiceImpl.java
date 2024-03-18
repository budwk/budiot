package com.budwk.app.iot.services.impl;

import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean(args = {"refer:dao"})
public class IotClassifyServiceImpl extends BaseServiceImpl<Iot_classify> implements IotClassifyService {
    public IotClassifyServiceImpl(Dao dao) {
        super(dao);
    }
}
