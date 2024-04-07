package com.budwk.app.access.protocol.codec.exception;

/**
 * 不支持的指令异常
 */
public class UnSupportCommandException extends RuntimeException {
    public UnSupportCommandException(String message) {
        super(message);
    }

    public UnSupportCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void makeThrow(String message, Object... args) {
        throw new UnSupportCommandException(String.format(message, args));
    }
}
