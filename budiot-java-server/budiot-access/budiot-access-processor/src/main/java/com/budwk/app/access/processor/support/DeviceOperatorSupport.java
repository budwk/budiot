package com.budwk.app.access.processor.support;

import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.impl.DefaultDeviceOperator;
import com.budwk.app.access.protocol.enums.ValveState;
import com.budwk.app.iot.enums.DeviceValveState;
import com.budwk.app.iot.services.IotDeviceService;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;


/**
 * 工具类
 */
@IocBean
public class DeviceOperatorSupport {
    @Inject
    private IotDeviceService iotDeviceService;
    public void flush(DeviceOperator operator) {
        if (!(operator instanceof DefaultDeviceOperator)) {
            return;
        }
        DefaultDeviceOperator dfOperator = (DefaultDeviceOperator) operator;
        if (Lang.isNotEmpty(dfOperator.getUpdatedProperties())) {
            // 更新 ext_property
            iotDeviceService.saveExtraProperties(dfOperator.getDeviceId(), dfOperator.getUpdatedProperties());
        }
    }

    private Integer convertValveState(ValveState valveState) {
        if (null == valveState) {
            return null;
        }
        switch (valveState) {
            case STATE_0:
                return DeviceValveState.NONE.value();
            case STATE_1:
                return DeviceValveState.ON.value();
            case STATE_2:
                return DeviceValveState.TEMPORARY_OFF.value();
            case STATE_3:
                return DeviceValveState.FORCE_OFF.value();
            case STATE_4:
                return DeviceValveState.UNKNOWN.value();
            case STATE_5:
                return DeviceValveState.FAULT.value();
        }
        return null;
    }
}
