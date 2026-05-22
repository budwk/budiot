package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceEventSourceType {
    DEVICE("DEVICE", "设备事件"), RULE("RULE", "规则告警");
    private final String value; private final String text;
    DeviceEventSourceType(String value, String text) { this.value = value; this.text = text; }
}
