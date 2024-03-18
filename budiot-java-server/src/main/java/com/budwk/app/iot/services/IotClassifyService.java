package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_classify;
import com.budwk.starter.database.service.BaseService;

public interface IotClassifyService extends BaseService<Iot_classify> {
    void save(Iot_classify classify, String pid);
    void deleteAndChild(Iot_classify classify);

}
