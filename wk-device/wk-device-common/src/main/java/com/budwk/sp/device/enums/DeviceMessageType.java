package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessageType {
    HEARTBEAT("HEARTBEAT", "心跳"),
    PROPERTY("PROPERTY", "属性上报"),
    EVENT("EVENT", "事件上报"),
    COMMAND("COMMAND", "指令下发"),
    RAW("RAW", "原始报文");
    private final String value; private final String text;
    DeviceMessageType(String value, String text) { this.value = value; this.text = text; }
}
