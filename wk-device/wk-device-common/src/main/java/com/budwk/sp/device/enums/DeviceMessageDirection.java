package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessageDirection {
    U("U", "上行"), D("D", "下行");
    private final String value; private final String text;
    DeviceMessageDirection(String value, String text) { this.value = value; this.text = text; }
}
