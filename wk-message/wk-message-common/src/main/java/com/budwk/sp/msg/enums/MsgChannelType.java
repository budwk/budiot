package com.budwk.sp.msg.enums;

import lombok.Getter;

@Getter
public enum MsgChannelType {
    SMS("SMS", "短信"),
    EMAIL("EMAIL", "邮箱"),
    DINGTALK("DINGTALK", "钉钉"),
    WECOM("WECOM", "企业微信");

    private final String value;
    private final String text;

    MsgChannelType(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
