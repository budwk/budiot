package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceNetworkProtocol {
    MQTT("MQTT", "MQTT"), TCP("TCP", "TCP"), UDP("UDP", "UDP"), HTTP("HTTP", "HTTP"), MODBUS_TCP("MODBUS_TCP", "Modbus TCP");
    private final String value; private final String text;
    DeviceNetworkProtocol(String value, String text) { this.value = value; this.text = text; }
}
