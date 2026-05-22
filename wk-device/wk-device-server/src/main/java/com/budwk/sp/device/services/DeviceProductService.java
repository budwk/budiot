package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceProductDTO;
import com.budwk.sp.device.dto.DeviceProductThingModelDTO;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;
import java.util.Map;

public interface DeviceProductService extends BaseService<Device_product> {
    Device_product createProduct(DeviceProductDTO dto, String operatorId, String tenantId);
    Device_product updateProduct(DeviceProductDTO dto, String operatorId, String tenantId);
    Device_product updateThingModel(DeviceProductThingModelDTO dto, String operatorId, String tenantId);
    void deleteProduct(String id, String tenantId);
    Device_product getProduct(String id, String tenantId);
    Device_product getByProductKey(String productKey, String tenantId);
    List<Device_product> listEnabled(String tenantId);
    Map<String, Integer> countDeviceByProductIds(List<String> productIds, String tenantId);
}
