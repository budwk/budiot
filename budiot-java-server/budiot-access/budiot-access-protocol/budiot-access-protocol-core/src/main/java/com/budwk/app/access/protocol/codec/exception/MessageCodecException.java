package com.budwk.app.access.protocol.codec.exception;

/**
 * 解析异常
 */
public class MessageCodecException extends RuntimeException {
    public MessageCodecException(String message) {
        super(message);
    }

    public MessageCodecException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void makeThrow(String message, Object... args) {
        throw new MessageCodecException(String.format(message, args));
    }
}
