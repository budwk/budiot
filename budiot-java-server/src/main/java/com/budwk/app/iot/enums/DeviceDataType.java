package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;
import org.nutz.lang.util.NutMap;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceDataType {
    INT(1, "整型"),
    FLOAT(2, "浮点型"),
    STRING(3, "字符串"),
    ENUM(4, "枚举型"),
    TIMESTAMP(5, "时间戳"),
    DATETIME(6, "日期时间"),
    IP(7, "IP地址");


    private int value;
    private String text;

    DeviceDataType(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public boolean isNumber() {
        return this.equals(INT) || this.equals(FLOAT) || this.equals(TIMESTAMP);
    }

    public static DeviceDataType from(int value) {
        for (DeviceDataType c : DeviceDataType.values()) {
            if (value == c.value) {
                return c;
            }
        }
        return null;
    }

    // excel导入用方法
    public static DeviceDataType fromText(String text) {
        for (DeviceDataType c : DeviceDataType.values()) {
            if (c.text().equals(text)) {
                return c;
            }
        }
        return null;
    }

    public static NutMap toMap() {
        NutMap map = NutMap.NEW();
        for (DeviceDataType v : DeviceDataType.values()) {
            map.put(String.valueOf(v.getValue()), v.getText());
        }
        return map;
    }

    public int value() {
        return value;
    }

    // excel导出用方法
    public String text() {
        return text;
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
}
