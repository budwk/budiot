package com.budwk.sp.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备错误消息状态
 */
@Getter
@AllArgsConstructor
public enum DeviceErrorMessageStatus {
    PENDING("PENDING", "待处理"),
    RETRYING("RETRYING", "重试中"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败");

    private final String value;
    private final String text;
}
