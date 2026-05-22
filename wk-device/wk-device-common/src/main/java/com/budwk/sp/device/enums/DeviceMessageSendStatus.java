package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceMessageSendStatus {
    PENDING("PENDING", "待发送"), SENT("SENT", "已发送"), FAILED("FAILED", "发送失败");
    private final String value; private final String text;
    DeviceMessageSendStatus(String value, String text) { this.value = value; this.text = text; }
}
