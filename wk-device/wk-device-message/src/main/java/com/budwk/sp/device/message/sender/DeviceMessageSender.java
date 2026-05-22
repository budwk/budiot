package com.budwk.sp.device.message.sender;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceMessageProviderType;

public interface DeviceMessageSender {
    DeviceMessageProviderType provider();
    void send(DeviceMessageEnvelope<?> envelope, String payloadJson);
}
