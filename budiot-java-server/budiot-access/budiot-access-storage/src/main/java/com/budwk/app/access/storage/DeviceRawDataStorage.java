package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceRawDataDTO;
import com.budwk.app.access.objects.query.DeviceRawDataQuery;
import org.nutz.lang.util.NutMap;

import java.util.List;

/**
 * 原始报文存储
 */
public interface DeviceRawDataStorage {
    void save(DeviceRawDataDTO data);

    NutMap list(DeviceRawDataQuery query);

    long count(DeviceRawDataQuery query);
}
