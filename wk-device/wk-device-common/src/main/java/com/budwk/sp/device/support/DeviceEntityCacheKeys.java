package com.budwk.sp.device.support;

import org.nutz.lang.Strings;

public final class DeviceEntityCacheKeys {
    private DeviceEntityCacheKeys() {
    }

    public static String product(String tenantId, String productId) {
        return "wk:device:cache:product:" + normalize(tenantId) + ":" + normalize(productId);
    }

    public static String device(String tenantId, String deviceId) {
        return "wk:device:cache:device:" + normalize(tenantId) + ":" + normalize(deviceId);
    }

    public static String protocol(String tenantId, String protocolId) {
        return "wk:device:cache:protocol:" + normalize(tenantId) + ":" + normalize(protocolId);
    }

    private static String normalize(String value) {
        return Strings.sBlank(value, "_").trim();
    }
}
