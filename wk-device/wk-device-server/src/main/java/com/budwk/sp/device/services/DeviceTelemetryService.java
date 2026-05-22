package com.budwk.sp.device.services;

import com.budwk.sp.device.entity.Device_event_log;
import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.starter.common.page.Pagination;

import java.util.List;
import java.util.Map;

public interface DeviceTelemetryService {
    List<Device_raw_log> listRawLogs(String deviceId, String tenantId, int limit);
    List<Device_raw_log> listRawLogs(String tenantId, long startAt, long endAt);
    List<Map<String, Object>> listDataLogs(String deviceId, String tenantId, int limit);
    List<Device_event_log> listEventLogs(String deviceId, String tenantId, int limit);
    Pagination pageRawLogs(String deviceId, String tenantId, int pageNo, int pageSize);
    Pagination pageRawLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize);
    Pagination pageDataLogs(String deviceId, String tenantId, int pageNo, int pageSize);
    Pagination pageDataLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize);
    Pagination pageEventLogs(String deviceId, String tenantId, int pageNo, int pageSize);
    Pagination pageEventLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize);
    long countRawLogs(String tenantId, long startAt, long endAt);
    long countHistoricalActiveDevices(String tenantId, long startAt, long endAt);
    long countEventLogs(String tenantId, long startAt, long endAt, List<DeviceEventSourceType> sourceTypes);
}
