package com.budwk.app.iot.commons.task;

import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.starter.job.annotation.SJob;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;

@IocBean(name = "deviceOfflineJob", singleton = false)
@Slf4j
public class DeviceOfflineJob {
    @Inject
    private IotDeviceService iotDeviceService;

    @SJob("check")
    public void check(String taskId, String params) {
        try {
            int minutes = 30;
            if (Strings.isNotBlank(params) && Strings.isNumber(params)) {
                minutes = Integer.parseInt(params);
            }
            iotDeviceService.doUpdateOffline(minutes);
        } catch (Exception e) {
            log.error("check device offline task error: {}，id: {}", e.getMessage(), taskId);
        }
    }
}
