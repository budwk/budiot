package com.budwk.sp.device.gateway.support;

import com.budwk.sp.device.dto.DeviceAccessDTO;
import com.budwk.sp.device.entity.Device_info;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

@Service
public class GatewayDeviceAccessService {
    private final Dao dao;

    public GatewayDeviceAccessService(Dao dao) {
        this.dao = dao;
    }

    public DeviceAccessDTO getAccessByIdentity(String tenantId, String identityType, String identityValue, String productKey) {
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
        return copy(dao.fetch(Device_info.class, cnd));
    }

    private String resolveIdentityField(String identityType) {
        return switch (Strings.sNull(identityType).trim().toUpperCase()) {
            case "IMEI" -> "imei";
            case "ICCID" -> "iccid";
            case "DEVICE_CODE", "" -> "deviceCode";
            default -> "deviceCode";
        };
    }

    private DeviceAccessDTO copy(Device_info info) {
        if (info == null) {
            return null;
        }
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
