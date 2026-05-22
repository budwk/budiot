package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceBatchCreateDTO;
import com.budwk.sp.device.dto.DeviceInfoDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface DeviceInfoService extends BaseService<Device_info> {
    Device_info createDevice(DeviceInfoDTO dto, String operatorId, String tenantId);
    Device_info updateDevice(DeviceInfoDTO dto, String operatorId, String tenantId);
    List<Device_info> batchCreate(DeviceBatchCreateDTO dto, String operatorId, String tenantId);
    void deleteDevice(String id, String tenantId);
    Device_info getDevice(String id, String tenantId);
    List<Device_info> getDevicesByIds(List<String> ids, String tenantId);
}
