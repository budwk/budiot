package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceProductType {
    DIRECT("DIRECT", "直连设备"), GATEWAY("GATEWAY", "网关设备"), GATEWAY_SUB("GATEWAY_SUB", "网关子设备");
    private final String value; private final String text;
    DeviceProductType(String value, String text) { this.value = value; this.text = text; }
}
