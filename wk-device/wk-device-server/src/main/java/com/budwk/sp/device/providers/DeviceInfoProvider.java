package com.budwk.sp.device.providers;

import com.budwk.sp.device.dto.DeviceAccessDTO;
import com.budwk.sp.device.entity.Device_info;
import org.apache.dubbo.config.annotation.DubboService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;

@DubboService(interfaceClass = IDeviceInfoProvider.class)
public class DeviceInfoProvider implements IDeviceInfoProvider {
    private final Dao dao;

    public DeviceInfoProvider(Dao dao) {
        this.dao = dao;
    }

    @Override
    public DeviceAccessDTO getAccessByDeviceCode(String tenantId, String deviceCode) {
        Device_info info = dao.fetch(Device_info.class, Cnd.where("tenantId", "=", tenantId)
                .and("deviceCode", "=", Strings.sNull(deviceCode).trim())
                .and("delFlag", "=", false));
        return copy(info);
    }

    @Override
    public DeviceAccessDTO getAccessByDeviceCode(String tenantId, String deviceCode, String productKey) {
        Device_info info = dao.fetch(Device_info.class, Cnd.where("tenantId", "=", tenantId)
                .and("deviceCode", "=", Strings.sNull(deviceCode).trim())
                .and("productKey", "=", Strings.sNull(productKey).trim())
                .and("delFlag", "=", false));
        return copy(info);
    }

    @Override
    public DeviceAccessDTO getAccessByIdentity(String tenantId, String identityType, String identityValue) {
        return copy(fetchByIdentity(tenantId, identityType, identityValue, null));
    }

    @Override
    public DeviceAccessDTO getAccessByIdentity(String tenantId, String identityType, String identityValue, String productKey) {
        return copy(fetchByIdentity(tenantId, identityType, identityValue, productKey));
    }

    @Override
    public DeviceAccessDTO getAccessById(String tenantId, String deviceId) {
        Device_info info = dao.fetch(Device_info.class, Cnd.where("tenantId", "=", tenantId)
                .and("id", "=", Strings.sNull(deviceId).trim())
                .and("delFlag", "=", false));
        return copy(info);
    }

    private Device_info fetchByIdentity(String tenantId, String identityType, String identityValue, String productKey) {
        String field = resolveIdentityField(identityType);
        if (Strings.isBlank(field) || Strings.isBlank(identityValue)) {
            return null;
        }
        Cnd cnd = Cnd.where("tenantId", "=", tenantId)
                .and(field, "=", Strings.sNull(identityValue).trim())
                .and("delFlag", "=", false);
        if (Strings.isNotBlank(productKey)) {
            cnd.and("productKey", "=", Strings.sNull(productKey).trim());
        }
        return dao.fetch(Device_info.class, cnd);
    }

    private String resolveIdentityField(String identityType) {
        String resolved = Strings.sNull(identityType).trim().toUpperCase();
        return switch (resolved) {
            case "IMEI" -> "imei";
            case "ICCID" -> "iccid";
            case "DEVICE_CODE", "" -> "deviceCode";
            default -> "deviceCode";
        };
    }

    private DeviceAccessDTO copy(Device_info info) {
        if (info == null) return null;
        DeviceAccessDTO dto = new DeviceAccessDTO();
        dto.setId(info.getId());
        dto.setTenantId(info.getTenantId());
        dto.setDeviceCode(info.getDeviceCode());
        dto.setProductId(info.getProductId());
        dto.setProductKey(info.getProductKey());
        dto.setSecretKey(info.getSecretKey());
        dto.setDisabled(info.isDisabled());
        return dto;
    }
}
