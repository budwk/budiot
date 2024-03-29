package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_product_attr;
import com.budwk.app.iot.models.Iot_product_cmd;
import com.budwk.starter.database.service.BaseService;

import java.util.List;

public interface IotProductCmdService extends BaseService<Iot_product_cmd> {
    String importData(String productId, String json, boolean over, String userId, String loginname);
}
