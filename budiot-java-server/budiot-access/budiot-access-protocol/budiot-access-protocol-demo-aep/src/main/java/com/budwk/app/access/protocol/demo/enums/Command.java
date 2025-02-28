package com.budwk.app.access.protocol.demo.enums;


import com.budwk.app.access.protocol.codec.exception.MessageCodecException;

public enum Command {
    /**
     * 阀门控制
     */
    VALVE_CONTROL,
    /**
     * 结束帧
     */
    END,
    /**
     * 查询参数
     */
    QUERY_PARAMS,
    /**
     * 设置参数
     */
    SET_PARAMS;

    public static Command from(String method) {
        try {
            return valueOf(method);
        } catch (Exception e) {
            throw new MessageCodecException("指令不存在 " + method);
        }
    }
}
