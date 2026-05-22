package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceCommandStatus {
    PENDING("PENDING", "待下发"), SENT("SENT", "已下发"), SUCCESS("SUCCESS", "执行成功"), FAILED("FAILED", "执行失败"), CANCELLED("CANCELLED", "已取消");
    private final String value; private final String text;
    DeviceCommandStatus(String value, String text) { this.value = value; this.text = text; }
}
