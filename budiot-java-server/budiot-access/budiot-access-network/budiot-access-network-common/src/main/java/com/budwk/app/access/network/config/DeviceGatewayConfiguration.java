package com.budwk.app.access.network.config;

import lombok.Data;

import java.util.Map;

@Data
public class DeviceGatewayConfiguration {
    /**
     * 网关标识
     */
    private String id;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 协议解析包标识
     */
    private String protocolCode;

    /**
     * 传输协议
     */
    private String transport;

    /**
     * 配置项
     */
    private Map<String, Object> properties;
}
