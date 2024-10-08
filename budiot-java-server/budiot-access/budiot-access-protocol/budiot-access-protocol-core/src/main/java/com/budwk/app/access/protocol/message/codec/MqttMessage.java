package com.budwk.app.access.protocol.message.codec;


import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.Data;

/**
 * @author wizzer.cn
 */
@Data
public class MqttMessage implements EncodedMessage {
    private static final long serialVersionUID = -5551990912154345413L;
    private final byte[] bytes;
    /**
     * 数据格式，默认是16进制，可选：hex 16进制（默认），string 字符串
     */
    private String payloadType = "hex";

    private String topic;

    private String clientId;

    private int qosLevel;

    private String messageId;

    private boolean will;

    private boolean dup;

    private boolean retain;

    private final String protocolCode;

    private String meaning;


    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
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
                "topic=" + topic +
                "clientId=" + clientId +
                "qosLevel=" + qosLevel +
                "messageId=" + messageId +
                "protocolCode=" + protocolCode +
                "will=" + will +
                "dup=" + dup +
                "retain=" + retain +
                "bytes=" + payloadAsString() +
                '}';
    }
}
