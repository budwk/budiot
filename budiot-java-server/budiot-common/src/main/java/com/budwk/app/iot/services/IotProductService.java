package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_product;
import com.budwk.starter.database.service.BaseService;

public interface IotProductService extends BaseService<Iot_product> {

    void create(Iot_product product);
}
