package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceType {
    METER("METER", "表具"),
    ALARM("ALARM", "报警器"),
    GATEWAY("GATEWAY", "网关"),
    COLLECTOR("COLLECTOR", "采集器"),
    DTU("COLLECTOR", "DTU"),
    VALVE("VALVE", "阀控"),
    ONOFF("ONOFF", "开关"),
    CAMARA("CAMARA", "摄像头"),
    HOME("HOME", "家居"),
    OTHER("OTHER", "其他");

    private String value;
    private String text;

    DeviceType(String value, String text) {
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

    public static DeviceType from(String value) {
        for (DeviceType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        return null;
    }
}
