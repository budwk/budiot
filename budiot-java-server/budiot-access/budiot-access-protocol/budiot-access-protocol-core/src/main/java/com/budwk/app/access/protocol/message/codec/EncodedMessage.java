package com.budwk.app.access.protocol.message.codec;


import com.budwk.app.access.protocol.enums.TransportType;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * 原始报文消息
 * @author wizzer.cn
 */
public interface EncodedMessage extends Serializable {
    /**
     * 是否已经下发过
     *
     * @return
     */
    default boolean isSend() {
        return false;
    }

    String getMessageId();

    TransportType getTransportType();

    /**
     * 获取解析器标识
     * @return
     */
    String getProtocolCode();

    byte[] getPayload();

    default String payloadAsString() {
        return new String(getPayload(), StandardCharsets.UTF_8);
    }
}
