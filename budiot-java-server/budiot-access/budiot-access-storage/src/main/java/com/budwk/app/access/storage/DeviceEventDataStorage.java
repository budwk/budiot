package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceEventDataDTO;
import com.budwk.app.access.objects.query.DeviceEventDataQuery;
import org.nutz.lang.util.NutMap;

import java.util.List;

/**
 * 设备事件存储
 */
public interface DeviceEventDataStorage {

    void create(int next);

    void save(DeviceEventDataDTO data);

    NutMap list(DeviceEventDataQuery query);

    long count(DeviceEventDataQuery query);
}
