package com.budwk.app.iot.services;

import com.budwk.app.iot.enums.SubscribeType;
import com.budwk.app.iot.models.Iot_product_sub;
import com.budwk.starter.database.service.BaseService;

import java.util.List;

public interface IotProductSubService extends BaseService<Iot_product_sub> {
    List<Iot_product_sub> getList(String productId, SubscribeType type);
}
