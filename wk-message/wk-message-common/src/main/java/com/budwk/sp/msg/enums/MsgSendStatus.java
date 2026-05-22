package com.budwk.sp.msg.enums;

import lombok.Getter;

@Getter
public enum MsgSendStatus {
    SUCCESS("SUCCESS", "发送成功"),
    FAIL("FAIL", "发送失败");

    private final String value;
    private final String text;

    MsgSendStatus(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
