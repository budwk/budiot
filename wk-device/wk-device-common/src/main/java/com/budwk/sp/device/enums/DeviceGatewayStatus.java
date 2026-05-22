package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceGatewayStatus {
    STOPPED("STOPPED", "已停止"),
    RUNNING("RUNNING", "运行中"),
    SUSPENDED("SUSPENDED", "已挂起"),
    ERROR("ERROR", "异常");

    private final String value;
    private final String text;

    DeviceGatewayStatus(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
