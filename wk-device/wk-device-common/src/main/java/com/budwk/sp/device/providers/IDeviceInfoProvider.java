package com.budwk.sp.device.providers;

import com.budwk.sp.device.dto.DeviceAccessDTO;

public interface IDeviceInfoProvider {
    DeviceAccessDTO getAccessByDeviceCode(String tenantId, String deviceCode);
    DeviceAccessDTO getAccessByDeviceCode(String tenantId, String deviceCode, String productKey);
    DeviceAccessDTO getAccessByIdentity(String tenantId, String identityType, String identityValue);
    DeviceAccessDTO getAccessByIdentity(String tenantId, String identityType, String identityValue, String productKey);
    DeviceAccessDTO getAccessById(String tenantId, String deviceId);
}
