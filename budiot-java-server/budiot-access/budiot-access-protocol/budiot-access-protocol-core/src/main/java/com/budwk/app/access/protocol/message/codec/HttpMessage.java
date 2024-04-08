package com.budwk.app.access.protocol.message.codec;

import com.budwk.app.access.protocol.enums.TransportType;
import lombok.Data;

import java.util.Map;

/**
 * @author wizzer.cn
 * @author zyang
 */
@Data
public class HttpMessage implements EncodedMessage {

    private static final long serialVersionUID = 1588297065657636014L;
    private final String protocolCode;
    private String messageId;
    private String platform;
    private byte[] payload;

    private Map<String, String> header;
    private String method;
    private String url;
    private String path;
    private Map<String, String> query;

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.HTTP;
    }

    @Override
    public String getProtocolCode() {
        return protocolCode;
    }

    @Override
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "HttpMessage{" +
                ", platform='" + platform + '\'' +
                ", protocolCode='" + protocolCode + '\'' +
                ", payload=" + payloadAsString() +
                ", header=" + header +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", query=" + query +
                '}';
    }
}
