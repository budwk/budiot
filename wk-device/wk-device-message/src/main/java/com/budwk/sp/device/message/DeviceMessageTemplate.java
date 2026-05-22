package com.budwk.sp.device.message;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceMessagePublishResultDTO;

import java.util.List;
import java.util.Map;

public interface DeviceMessageTemplate {
    DeviceMessagePublishResultDTO publish(DeviceMessageEnvelope<?> envelope, String bizId, String operatorId);
    DeviceMessagePublishResultDTO publishDownlinkCommand(DeviceDownlinkCommandMessageDTO message, String operatorId);
    Map<String, String> getStandardTopics();
    List<Map<String, String>> getSupportedProviders();
    List<Map<String, String>> getSupportedPatterns();
}
