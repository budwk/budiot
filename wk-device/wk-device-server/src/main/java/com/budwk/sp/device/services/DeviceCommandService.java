package com.budwk.sp.device.services;

import com.budwk.sp.device.dto.DeviceCommandDTO;
import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.entity.Device_command_log;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

public interface DeviceCommandService extends BaseService<Device_command> {
    List<Device_command> createCommands(DeviceCommandDTO dto, String operatorId, String tenantId);
    Device_command retryCommand(String id, String tenantId, String operatorId);
    void cancelCommand(String id, String tenantId, String operatorId);
    List<Device_command> listPending(String deviceId, String tenantId);
    List<Device_command_log> listLogs(String deviceId, String tenantId);
    Pagination pageLogs(String deviceId, String tenantId, int pageNo, int pageSize);
    Pagination pageLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize);
    Pagination pagePending(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize);
    void updateCommandStatus(DeviceCommandStatusUpdateDTO dto);
}
