package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceAttrType {
    INDEX(0, "指标"),
    VALVE(1, "阀门"),
    STATE(2, "状态"),
    INFO(3, "信息"),
    OTHER(4, "其他");
    private int value;
    private String text;

    DeviceAttrType(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int value() {
        return this.value;
    }

    public String text() {
        return this.text;
    }

    public static DeviceAttrType from(int value) {
        for (DeviceAttrType v : values()) {
            if (v.value() == value) {
                return v;
            }
        }
        return OTHER;
    }
}
