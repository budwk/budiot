package com.budwk.app.access.protocol.message.codec;


import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.Data;

/**
 * @author wizzer.cn
 * @author zyang
 */
@Data
public class TcpMessage implements EncodedMessage {
    private static final long serialVersionUID = -5874333451596406751L;
    private final byte[] bytes;
    /**
     * 数据格式，默认是16进制，可选：hex 16进制（默认），string 字符串
     */
    private String payloadType = "hex";
    private String messageId;
    private final String protocolCode;

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TCP;
    }

    @Override
    public String getProtocolCode() {
        return protocolCode;
    }

    @Override
    public byte[] getPayload() {
        return bytes;
    }

    @Override
    public String payloadAsString() {
        if ("hex".equals(payloadType)) {
            return ByteConvertUtil.bytesToHex(getPayload());
        }
        return EncodedMessage.super.payloadAsString();
    }

    @Override
    public String toString() {
        return "TcpMessage{" +
                "bytes=" + payloadAsString() +
                '}';
    }
}
