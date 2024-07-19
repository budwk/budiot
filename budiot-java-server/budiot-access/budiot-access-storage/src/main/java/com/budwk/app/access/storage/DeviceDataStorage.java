package com.budwk.app.access.storage;

import com.budwk.app.access.objects.dto.DeviceAttributeDTO;
import com.budwk.app.access.objects.dto.DeviceDataDTO;
import com.budwk.app.access.objects.query.DeviceDataQuery;
import org.nutz.lang.util.NutMap;

import java.util.List;
import java.util.Map;

/**
 * 设备数据存储
 */
public interface DeviceDataStorage {
    /**
     * 存储上报数据
     *
     * @param deviceDataDTO 设备基本信息
     * @param dataList      上报数据
     */
    void save(DeviceDataDTO deviceDataDTO, Map<String, Object> dataList);

    /**
     * 查询上报数据
     *
     * @param query 查询条件 handler不可为空
     * @return NutMap::: total,list
     */
    NutMap list(DeviceDataQuery query);

    /**
     * 查询统计
     *
     * @param query 查询条件 handler不可为空
     * @return
     */
    long count(DeviceDataQuery query);

    /**
     * 创建表结构(注意: 所有字段名为小写字母)
     *
     * @param deviceDataDTO 设备基本信息
     * @param attributeDTOS 设备上报参数
     */
    void createTable(DeviceDataDTO deviceDataDTO, List<DeviceAttributeDTO> attributeDTOS);
}
