package com.budwk.app.iot.enums;

import org.nutz.json.JsonShape;

@JsonShape(JsonShape.Type.OBJECT)
public enum SceneTermType {
    eq("eq", "等于"),
    neq("neq", "不等于"),
    gt("gt", "大于"),
    gte("gte", "大于等于"),
    lt("lt", "小于"),
    lte("lte", "小于等于"),
    btw("btw", "在...之间"),
    nbtw("nbtw", "不在...之间"),
    ;

    private String value;
    private String text;

    SceneTermType(String value, String text) {
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

    public static SceneTermType from(String value) {
        for (SceneTermType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown ProtocolType: " + value);
    }
}
