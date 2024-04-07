package com.budwk.app.access.protocol.codec.context;


import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.device.CommandInfo;

/**
 * 编码上下文
 */
public interface EncodeContext {
    /**
     * 指令信息
     */
    CommandInfo getCommandInfo();

    /**
     * 设备信息
     *
     * @return
     */
    DeviceOperator getDeviceOperator();

    /**
     * 缓存服务
     *
     * @param id
     * @return
     */
    CacheStore getCacheStore(String id);
}
