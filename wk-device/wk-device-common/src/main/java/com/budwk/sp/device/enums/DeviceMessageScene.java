package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessageScene {
    RAW_UPLINK("RAW_UPLINK", "原始上行报文"),
    NORMALIZED_UPLINK("NORMALIZED_UPLINK", "标准化上行消息"),
    COMMAND_DOWNLINK("COMMAND_DOWNLINK", "下行指令消息"),
    EVENT("EVENT", "事件告警消息"),
    RULE_FORWARD("RULE_FORWARD", "规则转发消息"),
    GATEWAY_CONTROL("GATEWAY_CONTROL", "网关控制消息");
    private final String value; private final String text;
    DeviceMessageScene(String value, String text) { this.value = value; this.text = text; }
}
