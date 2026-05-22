package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceEventLevel {
    INFO("INFO", "信息"), WARNING("WARNING", "警告"), CRITICAL("CRITICAL", "严重");
    private final String value; private final String text;
    DeviceEventLevel(String value, String text) { this.value = value; this.text = text; }
}
