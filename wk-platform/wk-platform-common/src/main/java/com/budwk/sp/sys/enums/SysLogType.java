package com.budwk.sp.sys.enums;

import org.nutz.json.JsonShape;

/**
 * 系统日志类型
 *
 * @author wizzer@qq.com
 */
@JsonShape(JsonShape.Type.OBJECT)
public enum SysLogType {
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "退出"),
    QUERY("QUERY", "查询"),
    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String text;

    SysLogType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public String value() {
        return value;
    }

    public String text() {
        return text;
    }
}
