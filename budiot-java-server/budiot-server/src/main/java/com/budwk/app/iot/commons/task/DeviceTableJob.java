package com.budwk.app.iot.commons.task;

import com.budwk.app.access.storage.DeviceCmdDataStorage;
import com.budwk.app.access.storage.DeviceEventDataStorage;
import com.budwk.app.access.storage.DeviceRawDataStorage;
import com.budwk.starter.job.annotation.SJob;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;

/**
 * 定时任务提前创建设备上报数据表
 */
@IocBean(name = "deviceTableJob", singleton = false)
@Slf4j
public class DeviceTableJob {
    @Inject
    private DeviceRawDataStorage deviceRawDataStorage;
    @Inject
    private DeviceCmdDataStorage deviceCmdDataStorage;
    @Inject
    private DeviceEventDataStorage deviceEventDataStorage;

    @SJob("createTable")
    public void createTable(String taskId, String params) {
        try {
            int next = 1;
            if (Strings.isNotBlank(params) && Strings.isNumber(params)) {
                next = Integer.parseInt(params);
            }
            deviceRawDataStorage.create(next);
            deviceCmdDataStorage.create(next);
            deviceEventDataStorage.create(next);
        } catch (Exception e) {
            log.error("check device offline task error: {}，id: {}", e.getMessage(), taskId);
        }
    }
}
