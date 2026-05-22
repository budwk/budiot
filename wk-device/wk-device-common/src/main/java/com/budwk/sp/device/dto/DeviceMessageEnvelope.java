package com.budwk.sp.device.dto;

import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "设备消息统一信封")
public class DeviceMessageEnvelope<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private String messageId;
    private String tenantId;
    private String productId;
    private String productKey;
    private String deviceId;
    private String deviceCode;
    private String gatewayNodeId;
    private DeviceMessageScene scene;
    private DeviceMessagePattern pattern = DeviceMessagePattern.TOPIC;
    private String topic;
    private String routingKey;
    private Integer delaySeconds;
    private Long occurredAt;
    private Map<String, String> headers = new HashMap<>();
    private T payload;
}
