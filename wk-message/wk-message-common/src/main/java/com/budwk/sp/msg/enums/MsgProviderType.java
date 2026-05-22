package com.budwk.sp.msg.enums;

import lombok.Getter;

@Getter
public enum MsgProviderType {
    ALIYUN_SMS("ALIYUN_SMS", "阿里云短信", MsgChannelType.SMS),
    TENCENT_SMS("TENCENT_SMS", "腾讯云短信", MsgChannelType.SMS),
    SMTP("SMTP", "SMTP邮箱", MsgChannelType.EMAIL),
    DINGTALK_BOT("DINGTALK_BOT", "钉钉机器人", MsgChannelType.DINGTALK),
    WECOM_BOT("WECOM_BOT", "企业微信机器人", MsgChannelType.WECOM);

    private final String value;
    private final String text;
    private final MsgChannelType channelType;

    MsgProviderType(String value, String text, MsgChannelType channelType) {
        this.value = value;
        this.text = text;
        this.channelType = channelType;
    }
}
