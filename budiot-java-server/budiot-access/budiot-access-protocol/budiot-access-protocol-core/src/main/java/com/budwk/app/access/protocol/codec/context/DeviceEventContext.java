package com.budwk.app.access.protocol.codec.context;


/**
 * 设备事件上下文
 */
public interface DeviceEventContext {
    /**
     * 更新设备iotPlatformId
     *
     * @param deviceId 设备ID
     * @param iotPlatformId 设备第三方平台ID
     */
    void updateDeviceIotPlatformId(String deviceId, String iotPlatformId);

}
