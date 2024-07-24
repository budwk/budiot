package com.budwk.app.access.protocol.codec;


import com.budwk.app.access.protocol.device.CommandInfo;
import com.budwk.app.access.protocol.device.ProductInfo;

/**
 * 资源装载类
 *
 * @author wizzer.cn
 */
public interface DeviceRegistry {
    /**
     * 通过指定字段获取网关设备
     * 如果是网关设备，调用这个接口获取
     *
     * @param protocolCode 协议标识
     * @param deviceNo     设备通信号
     * @return
     */
    DeviceOperator getGatewayDevice(String protocolCode, String deviceNo);

    /**
     * 通过设备通信号获取设备
     *
     * @param protocolCode 协议标识
     * @param deviceNo     设备通信号
     * @return
     */
    DeviceOperator getDeviceOperator(String protocolCode, String deviceNo);

    /**
     * 通过指定字段获取设备
     *
     * @param protocolCode 协议标识
     * @param fieldName    字段名称
     * @param fieldValue   字段值
     * @return
     */
    DeviceOperator getDeviceOperator(String protocolCode, String fieldName, String fieldValue);

    /**
     * 通过设备id获取设备
     *
     * @param deviceId 设备ID
     * @return
     */
    DeviceOperator getDeviceOperator(String deviceId);

    /**
     * 获取最早待下发指令
     *
     * @param deviceId 设备ID
     * @return
     */
    CommandInfo getDeviceCommand(String deviceId);

    ProductInfo getProductInfo(String productId);

    /**
     * 设置设备在线状态
     *
     * @param deviceId 设备ID
     */
    void updateDeviceOnline(String deviceId);

    void makeCommandSend(String cmdId);

    void makeCommandFinish(String cmdId, int status, String result);
}
