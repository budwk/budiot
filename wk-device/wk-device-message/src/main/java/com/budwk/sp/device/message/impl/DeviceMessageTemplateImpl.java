package com.budwk.sp.device.message.impl;

import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.dto.DeviceMessagePublishResultDTO;
import com.budwk.sp.device.enums.*;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.message.config.DeviceMessageProperties;
import com.budwk.sp.device.message.sender.DeviceMessageSender;
import com.budwk.sp.device.message.support.DeviceMessageTopics;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceMessageTemplateImpl implements DeviceMessageTemplate {
    private final ObjectMapper objectMapper;
    private final DeviceMessageProperties properties;
    private final Map<DeviceMessageProviderType, DeviceMessageSender> senderMap;

    public DeviceMessageTemplateImpl(ObjectMapper objectMapper, DeviceMessageProperties properties, List<DeviceMessageSender> senders) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.senderMap = senders.stream().collect(java.util.stream.Collectors.toMap(DeviceMessageSender::provider, item -> item, (a, b) -> a));
    }

    @Override
    public DeviceMessagePublishResultDTO publish(DeviceMessageEnvelope<?> envelope, String bizId, String operatorId) {
        if (!properties.isEnabled()) throw new BaseException("设备消息总线未启用");
        DeviceMessageEnvelope<?> resolved = prepareEnvelope(envelope);
        String payloadJson = toJson(resolved);
        try {
            DeviceMessageSender sender = senderMap.get(properties.getProvider());
            if (sender == null) throw new BaseException("未找到消息提供商实现：" + properties.getProvider().getValue());
            sender.send(resolved, payloadJson);
            return buildPublishResult(resolved, System.currentTimeMillis());
        } catch (Exception e) {
            String msg = Strings.sBlank(e.getMessage(), "设备消息发送失败");
            if (msg.length() > 250) msg = msg.substring(0, 250);
            throw new BaseException(msg);
        }
    }

    @Override
    public DeviceMessagePublishResultDTO publishDownlinkCommand(DeviceDownlinkCommandMessageDTO message, String operatorId) {
        DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(message.getTenantId());
        envelope.setProductId(message.getProductId());
        envelope.setProductKey(message.getProductKey());
        envelope.setDeviceId(message.getDeviceId());
        envelope.setDeviceCode(message.getDeviceCode());
        envelope.setGatewayNodeId(message.getGatewayNodeId());
        envelope.setScene(DeviceMessageScene.COMMAND_DOWNLINK);
        envelope.setPattern(DeviceMessagePattern.TOPIC);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.setRoutingKey(Strings.sBlank(message.getGatewayNodeId(), message.getDeviceCode()));
        envelope.getHeaders().put("networkProtocol", Strings.sBlank(message.getNetworkProtocol(), ""));
        envelope.getHeaders().put("commandCode", Strings.sBlank(message.getCommandCode(), ""));
        envelope.setPayload(message);
        return publish(envelope, message.getCommandId(), operatorId);
    }

    @Override
    public Map<String, String> getStandardTopics() {
        return DeviceMessageTopics.standardTopics(properties.getTopicPrefix());
    }

    @Override
    public List<Map<String, String>> getSupportedProviders() {
        return List.of(DeviceMessageProviderType.values()).stream().map(item -> Map.of("text", item.getText(), "value", item.getValue())).toList();
    }

    @Override
    public List<Map<String, String>> getSupportedPatterns() {
        return List.of(DeviceMessagePattern.values()).stream().map(item -> Map.of("text", item.getText(), "value", item.getValue())).toList();
    }

    private DeviceMessageEnvelope<?> prepareEnvelope(DeviceMessageEnvelope<?> envelope) {
        if (envelope == null || envelope.getScene() == null || envelope.getPayload() == null) throw new BaseException("设备消息内容不能为空");
        if (Strings.isBlank(envelope.getMessageId())) envelope.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        if (envelope.getOccurredAt() == null) envelope.setOccurredAt(System.currentTimeMillis());
        if (envelope.getPattern() == null) envelope.setPattern(DeviceMessagePattern.TOPIC);
        if (Strings.isBlank(envelope.getTopic())) envelope.setTopic(resolveTopic(envelope.getScene()));
        if (Strings.isBlank(envelope.getRoutingKey())) envelope.setRoutingKey(Strings.sBlank(envelope.getDeviceCode(), envelope.getScene().getValue().toLowerCase(Locale.ROOT)));
        return envelope;
    }

    private DeviceMessagePublishResultDTO buildPublishResult(DeviceMessageEnvelope<?> envelope, long publishedAt) {
        DeviceMessagePublishResultDTO result = new DeviceMessagePublishResultDTO();
        result.setMessageId(envelope.getMessageId());
        result.setTopic(envelope.getTopic());
        result.setRoutingKey(envelope.getRoutingKey());
        result.setPublishedAt(publishedAt);
        return result;
    }

    private String toJson(DeviceMessageEnvelope<?> envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new BaseException("设备消息序列化失败");
        }
    }

    private String resolveTopic(DeviceMessageScene scene) {
        return switch (scene) {
            case RAW_UPLINK -> DeviceMessageTopics.rawUplink(properties.getTopicPrefix());
            case NORMALIZED_UPLINK -> DeviceMessageTopics.normalizedUplink(properties.getTopicPrefix());
            case COMMAND_DOWNLINK -> DeviceMessageTopics.downlinkCommand(properties.getTopicPrefix());
            case EVENT -> DeviceMessageTopics.event(properties.getTopicPrefix());
            case RULE_FORWARD -> DeviceMessageTopics.ruleForward(properties.getTopicPrefix());
            case GATEWAY_CONTROL -> DeviceMessageTopics.gatewayBroadcast(properties.getTopicPrefix());
        };
    }
}
