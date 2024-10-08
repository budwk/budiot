package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

/**
 * 协议类型
 * @author wizzer.cn
 */
@JsonShape(JsonShape.Type.OBJECT)
public enum ProtocolType {
    TCP("TCP", "TCP"),
    UDP("UDP", "UDP"),
    MQ("MQ", "MQ"),
    MQTT("MQTT", "MQTT"),
    LWM2M("LWM2M", "LWM2M"),
    COAP("COAP", "COAP"),
    HTTP("HTTP", "HTTP"),
    MODBUS("MODBUS", "MODBUS");

    private String value;
    private String text;

    ProtocolType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public String value() {
        return value;
    }

    public String text() {
        return text;
    }

    public static ProtocolType from(String value) {
        for (ProtocolType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown ProtocolType: " + value);
    }
}
