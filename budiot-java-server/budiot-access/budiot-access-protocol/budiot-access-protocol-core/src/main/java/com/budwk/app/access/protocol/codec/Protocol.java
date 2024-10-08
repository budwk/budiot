package com.budwk.app.access.protocol.codec;

import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.protocol.codec.context.DeviceEventContext;

import java.util.List;

public interface Protocol {
    /**
     * 协议标识
     */
    String getCode();

    /**
     * 协议名称
     */
    String getName();

    /**
     * 获取支持的传输协议类型
     *
     * @return 支持的传输协议列表，如 TCP、UDP、HTTP等
     * @see TransportType
     */
    List<TransportType> getSupportedTransportTypes();

    /**
     * 获取指定传输协议的编解码实现
     *
     * @param transportType 传输协议类型 {@link  TransportType}
     * @return 编解码实现
     * @see MessageCodec
     */
    MessageCodec getMessageCodec(TransportType transportType);

    /**
     * 设备注册事件。当系统添加设备时会触发这个事件
     *
     * @param context        事件上下文
     * @param deviceOperator 设备操作类
     */
    default void onDeviceRegistered(DeviceEventContext context, DeviceOperator deviceOperator) {
    }

    /**
     * 设备注销事件。当系统删除设备时会触发这个事件
     *
     * @param context        事件上下文
     * @param deviceOperator 设备操作类
     */
    default void onDeviceUnRegistered(DeviceEventContext context, DeviceOperator deviceOperator) {
    }
}
