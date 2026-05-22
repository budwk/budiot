package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceBatchCreateDTO;
import com.budwk.sp.device.dto.DeviceInfoDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.services.DeviceInfoService;
import com.budwk.sp.device.services.DeviceProductService;
import com.budwk.sp.device.services.support.DeviceEntityRedisCacheService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceInfoServiceImpl extends BaseServiceImpl<Device_info> implements DeviceInfoService {
    private final DeviceProductService deviceProductService;
    private final DeviceEntityRedisCacheService deviceEntityRedisCacheService;

    public DeviceInfoServiceImpl(Dao dao, DeviceProductService deviceProductService, DeviceEntityRedisCacheService deviceEntityRedisCacheService) {
        super(dao);
        this.deviceProductService = deviceProductService;
        this.deviceEntityRedisCacheService = deviceEntityRedisCacheService;
    }

    @Override
    public Device_info createDevice(DeviceInfoDTO dto, String operatorId, String tenantId) {
        Device_product product = deviceProductService.getProduct(dto.getProductId(), tenantId);
        checkCodeUnique(null, tenantId, dto.getDeviceCode());
        Device_info info = build(dto, new Device_info(), operatorId, tenantId, product);
        info.setCreatedBy(operatorId);
        this.insert(info);
        deviceEntityRedisCacheService.cacheDevice(info);
        return info;
    }

    @Override
    public Device_info updateDevice(DeviceInfoDTO dto, String operatorId, String tenantId) {
        Device_info info = getDevice(dto.getId(), tenantId);
        Device_product product = deviceProductService.getProduct(dto.getProductId(), tenantId);
        checkCodeUnique(info.getId(), tenantId, dto.getDeviceCode());
        build(dto, info, operatorId, tenantId, product);
        this.updateIgnoreNull(info);
        deviceEntityRedisCacheService.cacheDevice(info);
        return info;
    }

    @Override
    public List<Device_info> batchCreate(DeviceBatchCreateDTO dto, String operatorId, String tenantId) {
        Device_product product = deviceProductService.getProduct(dto.getProductId(), tenantId);
        List<Device_info> list = new ArrayList<>();
        for (String code : dto.getDeviceCodes()) {
            String resolved = Strings.sNull(code).trim();
            if (Strings.isBlank(resolved)) continue;
            checkCodeUnique(null, tenantId, resolved);
            Device_info info = new Device_info();
            info.setTenantId(tenantId);
            info.setDeviceCode(resolved);
            info.setName(resolved);
            info.setProductId(product.getId());
            info.setProductKey(product.getProductKey());
            info.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
            info.setDisabled(dto.isDisabled());
            info.setCreatedBy(operatorId);
            info.setUpdatedBy(operatorId);
            this.insert(info);
            deviceEntityRedisCacheService.cacheDevice(info);
            list.add(info);
        }
        if (list.isEmpty()) throw new BaseException("没有可创建的设备编号");
        return list;
    }

    @Override
    public void deleteDevice(String id, String tenantId) {
        Device_info info = getDevice(id, tenantId);
        this.delete(info.getId());
        deviceEntityRedisCacheService.evictDevice(tenantId, info.getId());
    }

    @Override
    public Device_info getDevice(String id, String tenantId) {
        Device_info cached = deviceEntityRedisCacheService.getDevice(tenantId, id);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag())) {
            return cached;
        }
        Device_info info = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (info == null) throw new BaseException("设备不存在");
        deviceEntityRedisCacheService.cacheDevice(info);
        return info;
    }

    @Override
    public List<Device_info> getDevicesByIds(List<String> ids, String tenantId) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().map(id -> {
            try {
                return getDevice(id, tenantId);
            } catch (Exception e) {
                return null;
            }
        }).filter(java.util.Objects::nonNull).toList();
    }

    private Device_info build(DeviceInfoDTO dto, Device_info info, String operatorId, String tenantId, Device_product product) {
        info.setTenantId(tenantId);
        info.setDeviceCode(Strings.sNull(dto.getDeviceCode()).trim());
        info.setName(Strings.sBlank(dto.getName(), dto.getDeviceCode()).trim());
        info.setImei(Strings.sNull(dto.getImei()).trim());
        info.setIccid(Strings.sNull(dto.getIccid()).trim());
        info.setProductId(product.getId());
        info.setProductKey(product.getProductKey());
        info.setSecretKey(Strings.sBlank(dto.getSecretKey(), UUID.randomUUID().toString().replace("-", "")).trim());
        info.setDescription(Strings.sNull(dto.getDescription()).trim());
        info.setDisabled(dto.isDisabled());
        info.setUpdatedBy(operatorId);
        return info;
    }

    private void checkCodeUnique(String id, String tenantId, String deviceCode) {
        String resolved = Strings.sNull(deviceCode).trim();
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("deviceCode", "=", resolved).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) cnd.and("id", "<>", id);
        if (this.count(cnd) > 0) throw new BaseException("设备编号已存在");
    }
}
