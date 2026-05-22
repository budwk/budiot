package com.budwk.sp.device.services;

import java.util.Map;

public interface DeviceDashboardService {
    Map<String, Object> getDashboardData(String tenantId, String rangeType, Long startAt, Long endAt);
}
