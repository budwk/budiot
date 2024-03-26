package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceAttrType {
    INDEX(0, "指标"),
    STATE(1, "状态"),
    INFO(2, "信息"),
    OTHER(3, "其他");
    private int value;
    private String text;

    DeviceAttrType(int value, String text) {
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

    public static DeviceAttrType from(int value) {
        for (DeviceAttrType v : values()) {
            if (v.value() == value) {
                return v;
            }
        }
        return OTHER;
    }

    // excel导入用方法
    public static DeviceAttrType fromText(String text) {
        for (DeviceAttrType v : values()) {
            if (v.text().equals(text)) {
                return v;
            }
        }
        return OTHER;
    }
}
