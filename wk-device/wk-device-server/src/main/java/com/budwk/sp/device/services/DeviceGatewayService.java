package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceGatewayNodeDTO;
import com.budwk.sp.device.dto.DeviceGatewayDTO;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;
import java.util.Map;

public interface DeviceGatewayService extends BaseService<Device_gateway> {
    Device_gateway createGateway(DeviceGatewayDTO dto, String operatorId, String tenantId);
    Device_gateway updateGateway(DeviceGatewayDTO dto, String operatorId, String tenantId);
    void deleteGateway(String id, String tenantId);
    Device_gateway getGateway(String id, String tenantId);
    Device_gateway changeStatus(String id, String tenantId, DeviceGatewayStatus status, String operatorId);
    List<Device_gateway> listEnabled(String tenantId, DeviceNetworkProtocol protocol);
    Map<String, Device_gateway> getGatewayMap(List<String> ids, String tenantId);
    List<DeviceGatewayNodeDTO> listGatewayNodes(String tenantId, DeviceNetworkProtocol protocol);
}
