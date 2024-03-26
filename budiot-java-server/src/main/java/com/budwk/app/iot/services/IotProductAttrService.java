package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_product_attr;
import com.budwk.starter.database.service.BaseService;

import java.util.List;

public interface IotProductAttrService extends BaseService<Iot_product_attr> {
    String importData(String productId, String fileName, List<Iot_product_attr> list, boolean over, String userId, String loginname);

}
