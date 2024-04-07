package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceEventDataDTO;
import com.budwk.app.access.objects.query.DeviceEventDataQuery;

import java.util.List;

/**
 * 设备事件存储
 */
public interface DeviceEventDataStorage {
    void save(DeviceEventDataDTO data);

    List<DeviceEventDataDTO> list(DeviceEventDataQuery query);

    long count(DeviceEventDataQuery query);
}
