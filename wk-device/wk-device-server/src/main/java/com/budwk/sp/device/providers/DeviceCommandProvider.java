package com.budwk.sp.device.providers;

import com.budwk.sp.device.dto.DeviceCommandDTO;
import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.services.DeviceCommandService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService(interfaceClass = IDeviceCommandProvider.class)
public class DeviceCommandProvider implements IDeviceCommandProvider {
    private final DeviceCommandService deviceCommandService;

    public DeviceCommandProvider(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    @Override
    public List<Device_command> createCommands(DeviceCommandDTO dto, String operatorId, String tenantId) {
        return deviceCommandService.createCommands(dto, operatorId, tenantId);
    }

    @Override
    public void updateCommandStatus(DeviceCommandStatusUpdateDTO dto) {
        deviceCommandService.updateCommandStatus(dto);
    }
}
