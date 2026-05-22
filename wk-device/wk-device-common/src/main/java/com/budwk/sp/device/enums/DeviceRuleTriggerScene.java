package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceRuleTriggerScene {
    NORMALIZED_UPLINK("NORMALIZED_UPLINK", "标准化上行"),
    EVENT("EVENT", "事件告警");

    private final String value;
    private final String text;

    DeviceRuleTriggerScene(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
