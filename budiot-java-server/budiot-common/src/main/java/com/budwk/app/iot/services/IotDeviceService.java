package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import com.budwk.starter.database.service.BaseService;

import java.util.List;
import java.util.Map;

public interface IotDeviceService extends BaseService<Iot_device> {
    void importData(String productId, String fileName, List<Iot_device> list, boolean over, String userId, String loginname);

    /**
     * 更新设备扩展属性
     *
     * @param deviceId   设备ID
     * @param properties 属性配置
     */
    void saveExtraProperties(String deviceId, Map<String, Object> properties);

    void create(Iot_device device,Map<String, String> props);

    void delete(Iot_device device);

    DeviceProcessCache getCache(String deviceId);

    void setCache(Iot_device deviceInfo, Iot_product product);

    DeviceProcessCache doRefreshCache(String deviceId);

    void doUpdateCache(DeviceProcessCache device);
}
