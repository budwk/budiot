package com.budwk.app.access.processor.protocol.impl;

import com.budwk.app.access.protocol.codec.context.DeviceEventContext;
import com.budwk.app.iot.services.IotDeviceService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean
public class DefaultDeviceEventContext implements DeviceEventContext {
    @Inject
    private IotDeviceService iotDeviceService;

    @Override
    public void updateDeviceIotPlatformId(String deviceId, String iotPlatformId) {
        iotDeviceService.update(Chain.make("iotPlatformId", iotPlatformId), Cnd.where("id", "=", deviceId));
    }
}
