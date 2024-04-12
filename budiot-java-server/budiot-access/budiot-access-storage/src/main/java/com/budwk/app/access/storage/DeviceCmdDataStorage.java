package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceCmdDTO;
import com.budwk.app.access.objects.query.DeviceCmdDataQuery;

import java.util.List;

/**
 * 设备历史下发指令存储
 */
public interface DeviceCmdDataStorage {
    /**
     * 存储上报数据
     *
     * @param deviceCmdDTO 设备基本信息
     */
    void save(DeviceCmdDTO deviceCmdDTO);

    /**
     * 查询上报数据
     *
     * @param query 查询条件 handler不可为空
     * @return
     */
    List<DeviceCmdDTO> list(DeviceCmdDataQuery query);

    /**
     * 查询统计
     *
     * @param query 查询条件 handler不可为空
     * @return
     */
    long count(DeviceCmdDataQuery query);
}
