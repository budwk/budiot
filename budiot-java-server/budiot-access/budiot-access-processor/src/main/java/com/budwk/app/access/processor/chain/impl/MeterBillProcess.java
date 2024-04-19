package com.budwk.app.access.processor.chain.impl;

import com.budwk.app.access.processor.chain.AbstractDataProcessing;
import com.budwk.app.access.processor.chain.DeviceChainData;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * 表具计费业务处理
 */
@IocBean(singleton = false)
@Slf4j
public class MeterBillProcess extends AbstractDataProcessing {
    private DeviceProcessCache deviceProcessCache;
    @Override
    public void process(DeviceChainData deviceChainData) {
        deviceProcessCache = deviceChainData.getDeviceProcessCache();
        /**
         * todo
         */
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
