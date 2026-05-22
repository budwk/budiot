package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface DeviceProtocolService extends BaseService<Device_protocol> {
    Device_protocol createProtocol(DeviceProtocolDTO dto, String operatorId, String tenantId);
    Device_protocol updateProtocol(DeviceProtocolDTO dto, String operatorId, String tenantId);
    void deleteProtocol(String id, String tenantId);
    Device_protocol getProtocol(String id, String tenantId);
    List<Device_protocol> listEnabled(String tenantId);
    DeviceProtocolParseResultDTO debug(DeviceProtocolDebugRequestDTO request, String tenantId);
    String encodeCommand(String protocolId, String commandCode, String payloadJson, Device_info device, String tenantId);
}
