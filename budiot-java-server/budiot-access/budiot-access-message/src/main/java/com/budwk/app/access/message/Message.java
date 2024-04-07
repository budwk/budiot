package com.budwk.app.access.message;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class Message<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = -1160683315647739555L;
    @Getter
    @Setter(AccessLevel.NONE)
    private Map<String, String> headers = new LinkedHashMap<>();
    /**
     * 目的地址
     */
    private final String to;
    /**
     * 消息体
     */
    private final T body;
    /**
     * 回复地址
     */
    private String from;
    /**
     * 发送者的标识
     */
    private String sender;

    public Message<T> addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }
}
