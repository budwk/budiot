package com.budwk.sp.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceMessagePublishResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String topic;
    private String routingKey;
    private Long publishedAt;
}
