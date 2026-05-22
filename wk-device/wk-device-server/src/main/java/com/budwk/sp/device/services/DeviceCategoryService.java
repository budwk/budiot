package com.budwk.sp.device.services;

import com.budwk.sp.device.entity.Device_category;
import com.budwk.sp.starter.database.service.BaseService;

public interface DeviceCategoryService extends BaseService<Device_category> {
    void save(Device_category category, String parentId, String tenantId);
    void deleteAndChild(Device_category category, String tenantId);
    Device_category getCategory(String id, String tenantId);
}
