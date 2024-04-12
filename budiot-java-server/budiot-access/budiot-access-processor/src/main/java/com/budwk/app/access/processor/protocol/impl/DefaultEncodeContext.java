package com.budwk.app.access.processor.protocol.impl;

import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.DeviceRegistry;
import com.budwk.app.access.protocol.codec.context.EncodeContext;
import com.budwk.app.access.protocol.device.CommandInfo;
import lombok.Data;
import org.nutz.lang.Strings;

@Data
public class DefaultEncodeContext implements EncodeContext {
    private final CommandInfo commandInfo;
    private final DeviceOperator deviceOperator;

    private final DeviceRegistry deviceRegistry;

    public DefaultEncodeContext(DeviceRegistry deviceRegistry, CommandInfo commandInfo, DeviceOperator deviceOperator) {
        this.deviceRegistry = deviceRegistry;
        this.commandInfo = commandInfo;
        this.deviceOperator = deviceOperator;
    }

    @Override
    public CacheStore getCacheStore(String id) {
        return null;
    }

    @Override
    public DeviceOperator getDeviceOperator() {
        return deviceRegistry.getDeviceOperator(commandInfo.getDeviceId());
    }
}
