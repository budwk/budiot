package com.budwk.app.access.protocol.codec;

import com.budwk.app.access.enums.TransportType;

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

}
