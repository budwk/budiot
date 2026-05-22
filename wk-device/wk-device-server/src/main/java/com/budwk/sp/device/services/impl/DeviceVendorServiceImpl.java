package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceVendorDTO;
import com.budwk.sp.device.entity.Device_vendor;
import com.budwk.sp.device.services.DeviceVendorService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceVendorServiceImpl extends BaseServiceImpl<Device_vendor> implements DeviceVendorService {
    public DeviceVendorServiceImpl(Dao dao) { super(dao); }

    @Override
    public Device_vendor createVendor(DeviceVendorDTO dto, String operatorId, String tenantId) {
        checkCodeUnique(null, tenantId, dto.getCode());
        Device_vendor vendor = new Device_vendor();
        vendor.setTenantId(tenantId);
        vendor.setName(Strings.sNull(dto.getName()).trim());
        vendor.setCode(Strings.sNull(dto.getCode()).trim());
        vendor.setContactName(Strings.sNull(dto.getContactName()).trim());
        vendor.setContactMobile(Strings.sNull(dto.getContactMobile()).trim());
        vendor.setContactEmail(Strings.sNull(dto.getContactEmail()).trim());
        vendor.setDescription(Strings.sNull(dto.getDescription()).trim());
        vendor.setDisabled(dto.isDisabled());
        vendor.setCreatedBy(operatorId);
        vendor.setUpdatedBy(operatorId);
        this.insert(vendor);
        return vendor;
    }

    @Override
    public Device_vendor updateVendor(DeviceVendorDTO dto, String operatorId, String tenantId) {
        Device_vendor vendor = getVendor(dto.getId(), tenantId);
        checkCodeUnique(vendor.getId(), tenantId, dto.getCode());
        vendor.setName(Strings.sNull(dto.getName()).trim());
        vendor.setCode(Strings.sNull(dto.getCode()).trim());
        vendor.setContactName(Strings.sNull(dto.getContactName()).trim());
        vendor.setContactMobile(Strings.sNull(dto.getContactMobile()).trim());
        vendor.setContactEmail(Strings.sNull(dto.getContactEmail()).trim());
        vendor.setDescription(Strings.sNull(dto.getDescription()).trim());
        vendor.setDisabled(dto.isDisabled());
        vendor.setUpdatedBy(operatorId);
        this.updateIgnoreNull(vendor);
        return vendor;
    }

    @Override
    public void deleteVendor(String id, String tenantId) { this.delete(getVendor(id, tenantId).getId()); }

    @Override
    public Device_vendor getVendor(String id, String tenantId) {
        Device_vendor vendor = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (vendor == null) throw new BaseException("设备厂家不存在");
        return vendor;
    }

    @Override
    public List<Device_vendor> listEnabled(String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false).asc("createdAt"));
    }

    private void checkCodeUnique(String id, String tenantId, String code) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("code", "=", Strings.sNull(code).trim()).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) cnd.and("id", "<>", id);
        if (this.count(cnd) > 0) throw new BaseException("厂家编码已存在");
    }
}
