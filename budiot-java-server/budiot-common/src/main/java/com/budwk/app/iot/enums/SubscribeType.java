package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum SubscribeType {
    DATA(0, "数据上报"),
    EVENT(1, "事件上报");
    private int value;
    private String text;

    SubscribeType(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int value() {
        return this.value;
    }

    // excel导出用方法
    public String text() {
        return this.text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public static SubscribeType from(int value) {
        for (SubscribeType v : values()) {
            if (v.value() == value) {
                return v;
            }
        }
        return null;
    }

    // excel导入用方法
    public static SubscribeType fromText(String text) {
        for (SubscribeType v : values()) {
            if (v.text().equals(text)) {
                return v;
            }
        }
        return null;
    }
}
