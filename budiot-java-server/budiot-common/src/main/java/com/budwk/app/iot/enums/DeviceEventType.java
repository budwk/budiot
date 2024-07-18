package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceEventType {
    INFO(0, "信息"),
    ALARM(1, "报警"),
    FAULT(2, "故障"),
    RECOVER(3, "恢复");
    private int value;
    private String text;

    DeviceEventType(int value, String text) {
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

    public static DeviceEventType from(int value) {
        for (DeviceEventType v : values()) {
            if (v.value() == value) {
                return v;
            }
        }
        return null;
    }

    // excel导入用方法
    public static DeviceEventType fromText(String text) {
        for (DeviceEventType v : values()) {
            if (v.text().equals(text)) {
                return v;
            }
        }
        return null;
    }
}
