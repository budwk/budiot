package com.budwk.app.access.protocol.demo.enums;

import com.budwk.app.access.protocol.codec.exception.MessageCodecException;

public enum Command {
    /**
     * 阀门控制
     */
    VALVE_CONTROL;

    public static Command from(String code) {
        try {
            return valueOf(code);
        } catch (Exception e) {
            throw new MessageCodecException("指令不存在 " + code);
        }
    }
}
