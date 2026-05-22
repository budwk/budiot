package com.budwk.sp.device.enums;

import lombok.Getter;

@Getter
public enum DeviceGatewayMode {
    MQTT_SERVER("MQTT_SERVER", "MQTT服务端"),
    MQTT_CLIENT("MQTT_CLIENT", "MQTT客户端"),
    TCP_SERVER("TCP_SERVER", "TCP服务端"),
    UDP_SERVER("UDP_SERVER", "UDP服务端"),
    HTTP_SERVER("HTTP_SERVER", "HTTP服务端"),
    MODBUS_TCP_SERVER("MODBUS_TCP_SERVER", "Modbus TCP服务端");

    private final String value;
    private final String text;

    DeviceGatewayMode(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
