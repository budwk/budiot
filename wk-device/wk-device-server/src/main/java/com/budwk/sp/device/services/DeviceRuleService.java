package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceRuleDTO;
import com.budwk.sp.device.entity.Device_rule;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.Map;

public interface DeviceRuleService extends BaseService<Device_rule> {
    Device_rule createRule(DeviceRuleDTO dto, String operatorId, String tenantId);

    Device_rule updateRule(DeviceRuleDTO dto, String operatorId, String tenantId);

    void deleteRule(String id, String tenantId);

    Device_rule getRule(String id, String tenantId);

    Map<String, Object> getBaseData(String tenantId);

    Map<String, Object> getProductMeta(String productId, String tenantId);
}
