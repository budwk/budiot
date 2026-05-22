package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceProtocolScriptType {
    JAVASCRIPT("JAVASCRIPT", "JavaScript (GraalJS)");
    private final String value; private final String text;
    DeviceProtocolScriptType(String value, String text) { this.value = value; this.text = text; }
}
