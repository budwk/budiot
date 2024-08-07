package com.budwk.app.access.protocol.enums;

/**
 * 报文含义，用于特定行业业务需要，如水表、电表等
 */
public enum MeaningState {
    HEART(0, "心跳"),
    REGISTER(1, "注册"),
    REGISTER_BACK(2, "注册响应"),
    ACTIVE_REPORT(3, "读数上报"),
    VALVE_CONTROL_REPORT(4, "阀门改变上报"),
    GET_ABNORMAL_FLOW(5, "读异常流量记录"),
    VALVE_CONTROL(6, "阀控");

    private final int value;
    private final String text;

    MeaningState(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int value() {
        return value;
    }

    public String text() {
        return text;
    }

    public static MeaningState from(int value) {
        return values()[value];
    }

    public static String getTextFromName(String name) {
        MeaningState[] meaningStates = MeaningState.values();
        for (MeaningState meaningState : meaningStates) {
            if (name.equals(meaningState.name())) {
                return meaningState.text;
            }
        }
        return null;
    }
}
