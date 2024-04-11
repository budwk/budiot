package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;
import org.nutz.lang.util.NutMap;

@JsonShape(JsonShape.Type.OBJECT)
public enum DeviceCmdStatus {
    WAIT(0, "待下发"),
    SEND(1, "已下发"),
    DELIVERED(2, "已送达"),
    FINISHED(3, "已完成"),
    ERROR(4, "下发失败"),
    FAILED(5, "执行失败"),
    CANCELED(6, "已取消"),
    TIMEOUT(7, "超时"),
    EXPIRED(8, "已过期");


    private int value;
    private String text;

    DeviceCmdStatus(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static DeviceCmdStatus from(int value) {
        for (DeviceCmdStatus c : DeviceCmdStatus.values()) {
            if (value == c.value) {
                return c;
            }
        }
        throw new IllegalArgumentException("unknown DeviceCmdStatus: " + value);
    }

    public static NutMap toMap() {
        NutMap map = NutMap.NEW();
        for (DeviceCmdStatus v : DeviceCmdStatus.values()) {
            map.put(String.valueOf(v.getValue()), v.getText());
        }
        return map;
    }

    public int value() {
        return value;
    }

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
