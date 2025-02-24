package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum SceneActionType {
    CMD("CMD", "设备指令"),
    SMS("SMS", "短信通知"),
    EMAIL("EMAIL", "邮件通知"),
    ;

    private String value;
    private String text;

    SceneActionType(String value, String text) {
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

    public static SceneActionType from(String value) {
        for (SceneActionType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown ProtocolType: " + value);
    }
}
