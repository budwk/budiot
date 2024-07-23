package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceCmdDTO;
import com.budwk.app.access.objects.query.DeviceCmdDataQuery;
import org.nutz.lang.util.NutMap;

import java.util.List;

/**
 * 设备历史下发指令存储
 */
public interface DeviceCmdDataStorage {
    /**
     * 提前创建集合
     * @param next 未来N年
     */
    void create(int next);
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
    NutMap list(DeviceCmdDataQuery query);

    /**
     * 查询统计
     *
     * @param query 查询条件 handler不可为空
     * @return
     */
    long count(DeviceCmdDataQuery query);
}
