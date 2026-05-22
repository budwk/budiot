package com.budwk.sp.msg.enums;

import lombok.Getter;

@Getter
public enum MsgTemplateBizType {
    VERIFY_CODE("VERIFY_CODE", "验证码"),
    NOTIFY("NOTIFY", "通知");

    private final String value;
    private final String text;

    MsgTemplateBizType(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
