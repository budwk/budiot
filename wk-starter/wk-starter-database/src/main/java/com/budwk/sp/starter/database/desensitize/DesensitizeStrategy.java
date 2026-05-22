package com.budwk.sp.starter.database.desensitize;

import java.util.function.Function;

/**
 * 脱敏策略枚举
 */
public enum DesensitizeStrategy {
    // 手机号：前3位，后4位，中间保留
    MOBILE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),
    // 邮箱：仅显示第一个字母和@后面的部分
    EMAIL(s -> s.replaceAll("(^.{1}).*(@.*$)", "$1****$2")),
    // 身份证：前6位，后4位
    ID_CARD(s -> s.replaceAll("(\\d{6})\\d{8,11}(\\w{4})", "$1**********$2")),
    // 姓名：只显示第一个汉字
    CHINESE_NAME(s -> s.replaceAll("(?<=.).", "*"));

    private final Function<String, String> desensitizer;

    DesensitizeStrategy(Function<String, String> desensitizer) {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> getDesensitizer() {
        return desensitizer;
    }
}