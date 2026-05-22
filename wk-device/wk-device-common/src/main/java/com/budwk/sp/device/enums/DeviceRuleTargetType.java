package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceRuleTargetType {
    SMS("SMS", "短信"),
    SITE_MESSAGE("SITE_MESSAGE", "站内信"),
    SCENE_LINKAGE("SCENE_LINKAGE", "场景联动"),
    HTTP_PUSH("HTTP_PUSH", "HTTP推送"),
    QUEUE_PUSH("QUEUE_PUSH", "队列推送");

    private final String value;
    private final String text;

    DeviceRuleTargetType(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
