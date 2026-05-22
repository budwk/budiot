package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessagePattern {
    TOPIC("TOPIC", "主题消息"), DELAY("DELAY", "延迟消息"), BROADCAST("BROADCAST", "广播消息");
    private final String value; private final String text;
    DeviceMessagePattern(String value, String text) { this.value = value; this.text = text; }
}
