package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceGatewayAgentDTO;
import com.budwk.sp.device.dto.DeviceGatewayNodeDTO;
import com.budwk.sp.device.dto.DeviceRuntimeDTO;

import java.util.List;

public interface DeviceRuntimeService {
    DeviceRuntimeDTO getRuntime(String tenantId, String deviceId);
    List<DeviceGatewayNodeDTO> listGatewayNodes(String tenantId);
    List<DeviceGatewayAgentDTO> listGatewayAgents(String tenantId);
}
