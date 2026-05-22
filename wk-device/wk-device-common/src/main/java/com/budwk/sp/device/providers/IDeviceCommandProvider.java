package com.budwk.sp.device.providers;

import com.budwk.sp.device.dto.DeviceCommandDTO;
import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.entity.Device_command;

import java.util.List;

public interface IDeviceCommandProvider {
    List<Device_command> createCommands(DeviceCommandDTO dto, String operatorId, String tenantId);
    void updateCommandStatus(DeviceCommandStatusUpdateDTO dto);
}
