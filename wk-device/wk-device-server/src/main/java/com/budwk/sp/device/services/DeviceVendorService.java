package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceVendorDTO;
import com.budwk.sp.device.entity.Device_vendor;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface DeviceVendorService extends BaseService<Device_vendor> {
    Device_vendor createVendor(DeviceVendorDTO dto, String operatorId, String tenantId);
    Device_vendor updateVendor(DeviceVendorDTO dto, String operatorId, String tenantId);
    void deleteVendor(String id, String tenantId);
    Device_vendor getVendor(String id, String tenantId);
    List<Device_vendor> listEnabled(String tenantId);
}
