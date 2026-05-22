package com.budwk.sp.msg.sender.support;

import java.util.Map;

public final class MsgContentHelper {
    private MsgContentHelper() {
    }

    public static String render(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String rendered = template;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                rendered = rendered.replace(key, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return rendered;
    }
}
