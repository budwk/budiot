package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.dto.*;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.enums.*;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.message.support.DeviceMessageTopics;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeviceRawMessageHandlerService {
    private final Dao dao;
    private final ObjectMapper objectMapper;
    private final WkCacheService wkCacheService;
    private final DeviceMessageTemplate deviceMessageTemplate;
    private final DeviceEntityRedisCacheService deviceEntityRedisCacheService;
    private final DeviceProtocolScriptCacheService scriptCacheService;
    private final DeviceScriptExecutionService scriptExecutionService;
    private final HandlerCommandArchiveService handlerCommandArchiveService;
    private final boolean relationalRawEnabled;
    private final String rawArchiveTopic;

    public DeviceRawMessageHandlerService(Dao dao,
                                          ObjectMapper objectMapper,
                                          WkCacheService wkCacheService,
                                          DeviceMessageTemplate deviceMessageTemplate,
                                          DeviceEntityRedisCacheService deviceEntityRedisCacheService,
                                          DeviceProtocolScriptCacheService scriptCacheService,
                                          HandlerCommandArchiveService handlerCommandArchiveService,
                                          DeviceScriptExecutionService scriptExecutionService,
                                          @Value("${wk.device.database-ext.storage.raw:DEFAULT}") String rawStorage,
                                          @Value("${wk.device.database-ext.raw-archive-topic:wk.device.uplink.raw.archive}") String rawArchiveTopic) {
        this.dao = dao;
        this.objectMapper = objectMapper;
        this.wkCacheService = wkCacheService;
        this.deviceMessageTemplate = deviceMessageTemplate;
        this.deviceEntityRedisCacheService = deviceEntityRedisCacheService;
        this.scriptCacheService = scriptCacheService;
        this.handlerCommandArchiveService = handlerCommandArchiveService;
        this.scriptExecutionService = scriptExecutionService;
        this.relationalRawEnabled = "DEFAULT".equalsIgnoreCase(Strings.sBlank(rawStorage, "DEFAULT"));
        this.rawArchiveTopic = Strings.sBlank(rawArchiveTopic, DeviceMessageTopics.rawUplink("wk.device") + ".archive");
    }

    public void handle(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope) {
        Device_info device = deviceEntityRedisCacheService.getDevice(envelope.getTenantId(), envelope.getDeviceId());
        Device_product product = deviceEntityRedisCacheService.getProduct(envelope.getTenantId(), envelope.getProductId());
        if (device == null) throw new BaseException("设备不存在");
        if (product == null) throw new BaseException("设备产品不存在");
        Device_protocol protocol = scriptCacheService.getProtocol(envelope.getTenantId(), product.getProtocolId());
        if (protocol == null) {
            throw new BaseException("设备协议不存在或已禁用");
        }
        try {
            DeviceProtocolParseResultDTO result = scriptExecutionService.execute(protocol, buildScriptInput(envelope, product, protocol));
            long deviceAt = result.getDeviceAt() == null ? System.currentTimeMillis() : result.getDeviceAt();
            String parsedJson = serializeParsedResult(result, deviceAt);
            publishRawArchive(envelope, envelope.getPayload() == null ? protocol.getCode() : envelope.getPayload().getProtocol(), parsedJson, true, deviceAt);
            publishNormalizedMessages(envelope, result, deviceAt);
            publishEvents(envelope, result, deviceAt);
            recordReplies(envelope, device, product, protocol, result);
            updateRuntime(envelope, result, deviceAt);
            clearRuntimeError(envelope.getTenantId(), envelope.getDeviceId());
        } catch (BaseException e) {
            long deviceAt = envelope.getPayload() != null && envelope.getPayload().getReceivedAt() != null ? envelope.getPayload().getReceivedAt() : System.currentTimeMillis();
            String parsedJson = buildErrorParsedJson(envelope, e, deviceAt);
            publishRawArchive(envelope, envelope.getPayload() == null ? protocol.getCode() : envelope.getPayload().getProtocol(), parsedJson, false, deviceAt);
            updateRuntimeError(envelope.getTenantId(), envelope.getDeviceId(), e.getMessage());
        } finally {
            dispatchPendingCommands(device, product, protocol);
        }
    }

    private Map<String, Object> buildScriptInput(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, Device_product product, Device_protocol protocol) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("direction", "U");
        map.put("messageId", envelope.getMessageId());
        map.put("tenantId", envelope.getTenantId());
        map.put("productId", envelope.getProductId());
        map.put("productKey", envelope.getProductKey());
        map.put("productName", product.getName());
        map.put("deviceId", envelope.getDeviceId());
        map.put("deviceCode", envelope.getDeviceCode());
        map.put("gatewayNodeId", envelope.getGatewayNodeId());
        map.put("protocolId", protocol.getId());
        map.put("protocolCode", protocol.getCode());
        map.put("receivedAt", envelope.getOccurredAt());
        map.put("headers", envelope.getHeaders());
        if (envelope.getPayload() != null) {
            map.put("networkProtocol", envelope.getPayload().getProtocol());
            map.put("endpoint", envelope.getPayload().getEndpoint());
            map.put("sourceIp", envelope.getPayload().getSourceIp());
            map.put("payload", envelope.getPayload().getPayload());
            map.put("payloadText", envelope.getPayload().getPayload());
        }
        return map;
    }

    private void publishNormalizedMessages(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, DeviceProtocolParseResultDTO result, long deviceAt) {
        if (result.getProperties() == null || result.getProperties().isEmpty()) {
            return;
        }
        for (DeviceParsedPropertyDTO property : result.getProperties()) {
            DeviceUplinkMessageDTO payload = new DeviceUplinkMessageDTO();
            payload.setIdentifier(Strings.sBlank(property.getIdentifier(), "property"));
            payload.setMessageType(Strings.sBlank(result.getMessageType(), DeviceMessageType.PROPERTY.getValue()));
            payload.setDataJson(Strings.sBlank(property.getValueJson(), "{}"));
            payload.setMetadataJson(buildMetadata(result.getMetadataJson(), property));
            payload.setDeviceAt(property.getDeviceAt() == null ? deviceAt : property.getDeviceAt());
            publishNormalizedEnvelope(envelope, payload, property.getIdentifier());
        }
    }

    private void publishNormalizedEnvelope(DeviceMessageEnvelope<DeviceRawMessageDTO> source, DeviceUplinkMessageDTO payload, String bizId) {
        DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(source.getTenantId());
        envelope.setProductId(source.getProductId());
        envelope.setProductKey(source.getProductKey());
        envelope.setDeviceId(source.getDeviceId());
        envelope.setDeviceCode(source.getDeviceCode());
        envelope.setGatewayNodeId(source.getGatewayNodeId());
        envelope.setScene(DeviceMessageScene.NORMALIZED_UPLINK);
        envelope.setPattern(DeviceMessagePattern.TOPIC);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.setMessageId(buildDataMessageId(source.getMessageId(), Strings.sBlank(payload.getIdentifier(), bizId)));
        envelope.setRoutingKey(source.getDeviceCode());
        envelope.getHeaders().putAll(source.getHeaders());
        envelope.getHeaders().put("sourceMessageId", Strings.sBlank(source.getMessageId(), ""));
        envelope.setPayload(payload);
        deviceMessageTemplate.publish(envelope, Strings.sBlank(bizId, source.getDeviceId()), "handler");
    }

    private void publishEvents(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, DeviceProtocolParseResultDTO result, long deviceAt) {
        if (result.getEvents() == null) return;
        for (DeviceParsedEventDTO event : result.getEvents()) {
            DeviceEventMessageDTO payload = new DeviceEventMessageDTO();
            payload.setEventCode(Strings.sNull(event.getEventCode()).trim());
            payload.setEventName(Strings.sBlank(event.getEventName(), event.getEventCode()).trim());
            payload.setLevel(parseEventLevel(event.getLevel()).getValue());
            payload.setSourceType(parseSourceType(event.getSourceType()).getValue());
            payload.setContentJson(Strings.sBlank(event.getContentJson(), "{}"));
            payload.setDeviceAt(event.getDeviceAt() == null ? deviceAt : event.getDeviceAt());
            DeviceMessageEnvelope<DeviceEventMessageDTO> eventEnvelope = new DeviceMessageEnvelope<>();
            eventEnvelope.setTenantId(envelope.getTenantId());
            eventEnvelope.setProductId(envelope.getProductId());
            eventEnvelope.setProductKey(envelope.getProductKey());
            eventEnvelope.setDeviceId(envelope.getDeviceId());
            eventEnvelope.setDeviceCode(envelope.getDeviceCode());
            eventEnvelope.setGatewayNodeId(envelope.getGatewayNodeId());
            eventEnvelope.setScene(DeviceMessageScene.EVENT);
            eventEnvelope.setPattern(DeviceMessagePattern.TOPIC);
            eventEnvelope.setOccurredAt(System.currentTimeMillis());
            eventEnvelope.setRoutingKey(envelope.getDeviceCode());
            eventEnvelope.getHeaders().putAll(envelope.getHeaders());
            eventEnvelope.setPayload(payload);
            deviceMessageTemplate.publish(eventEnvelope, Strings.sBlank(payload.getEventCode(), envelope.getDeviceId()), "handler");
        }
    }

    private void recordReplies(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, Device_info device, Device_product product, Device_protocol protocol, DeviceProtocolParseResultDTO result) {
        if (result.getReplies() == null) return;
        for (DeviceProtocolReplyDTO reply : result.getReplies()) {
            String commandCode = Strings.sBlank(reply.getCommandCode(), "AUTO_REPLY");
            String storedPayload = Strings.isNotBlank(reply.getPayloadJson()) ? reply.getPayloadJson() :
                    (Strings.isNotBlank(reply.getPayload()) ? "{\"payload\":\"" + Strings.sNull(reply.getPayload()).trim() + "\"}" : "{}");
            String downlinkPayload = Strings.isNotBlank(reply.getPayload()) ? Strings.sNull(reply.getPayload()).trim()
                    : scriptExecutionService.encode(protocol, commandCode, storedPayload, device);
            DeviceDownlinkCommandMessageDTO payload = new DeviceDownlinkCommandMessageDTO();
            payload.setCommandId("");
            payload.setCommandLogId("");
            payload.setTenantId(device.getTenantId());
            payload.setProductId(device.getProductId());
            payload.setProductKey(product.getProductKey());
            payload.setDeviceId(device.getId());
            payload.setDeviceCode(device.getDeviceCode());
            payload.setGatewayNodeId(Strings.sBlank(product.getGatewayNodeId(), envelope.getGatewayNodeId()));
            payload.setNetworkProtocol(product.getNetworkProtocol() == null ? "" : product.getNetworkProtocol().getValue());
            payload.setCommandCode(commandCode);
            payload.setPayload(downlinkPayload);
            payload.setPayloadJson(storedPayload);
            payload.setArchiveCommandHistory(false);
            payload.setReplyRequired(reply.isReplyRequired());
            payload.setDeadlineAt(reply.getDeadlineAt());
            deviceMessageTemplate.publishDownlinkCommand(payload, "handler");
        }
    }

    private void updateRuntime(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, DeviceProtocolParseResultDTO result, long deviceAt) {
        wkCacheService.putHashValue(runtimeKey(envelope.getTenantId(), envelope.getDeviceId()), "lastDeviceAt", String.valueOf(deviceAt));
        if (DeviceMessageType.HEARTBEAT.getValue().equalsIgnoreCase(result.getMessageType())) {
            wkCacheService.putHashValue(runtimeKey(envelope.getTenantId(), envelope.getDeviceId()), "lastHeartbeatAt", String.valueOf(deviceAt));
        }
    }

    private void updateRuntimeError(String tenantId, String deviceId, String error) {
        wkCacheService.putHashValue(runtimeKey(tenantId, deviceId), "lastError", Strings.sBlank(error, ""));
    }

    private void clearRuntimeError(String tenantId, String deviceId) {
        wkCacheService.putHashValue(runtimeKey(tenantId, deviceId), "lastError", "");
    }

    private String buildMetadata(String metadataJson, DeviceParsedPropertyDTO property) {
        try {
            Map<String, Object> metadata = Strings.isBlank(metadataJson) ? new LinkedHashMap<>() : objectMapper.readValue(metadataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            metadata.put("name", Strings.sBlank(property.getName(), property.getIdentifier()));
            metadata.put("unit", Strings.sBlank(property.getUnit(), ""));
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String serializeParsedResult(DeviceProtocolParseResultDTO result, long deviceAt) {
        try {
            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("messageType", Strings.sBlank(result.getMessageType(), DeviceMessageType.RAW.getValue()));
            parsed.put("deviceAt", deviceAt);
            parsed.put("metadataJson", Strings.sBlank(result.getMetadataJson(), ""));
            parsed.put("properties", result.getProperties() == null ? List.of() : result.getProperties());
            parsed.put("events", result.getEvents() == null ? List.of() : result.getEvents());
            parsed.put("replies", result.getReplies() == null ? List.of() : result.getReplies());
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            return "";
        }
    }

    private String buildErrorParsedJson(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope, BaseException error, long deviceAt) {
        try {
            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("messageType", DeviceMessageType.RAW.getValue());
            parsed.put("deviceAt", deviceAt);
            parsed.put("error", Strings.sBlank(error.getMessage(), "解析失败"));
            parsed.put("payload", envelope.getPayload() == null ? "" : Strings.sBlank(envelope.getPayload().getPayload(), ""));
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            return "";
        }
    }

    private void dispatchPendingCommands(Device_info device, Device_product product, Device_protocol protocol) {
        if (device == null) {
            return;
        }
        List<Device_command> commands = dao.query(Device_command.class, Cnd.where("tenantId", "=", device.getTenantId())
                .and("deviceId", "=", device.getId())
                .and("status", "=", DeviceCommandStatus.PENDING)
                .and("delFlag", "=", false)
                .asc("createdAt"));
        for (Device_command command : commands) {
            if (Strings.isNotBlank(command.getMessageId())) {
                continue;
            }
            try {
                long now = System.currentTimeMillis();
                if (isExpired(command, now)) {
                    handlerCommandArchiveService.expirePendingCommand(command, device, now);
                    log.info("待下发指令已在设备上报时判定超时并归档: tenantId={}, deviceId={}, commandId={}",
                            device.getTenantId(), device.getId(), command.getId());
                    continue;
                }
                String downlinkPayload = resolvePendingDownlinkPayload(command, device, protocol);
                DeviceDownlinkCommandMessageDTO payload = new DeviceDownlinkCommandMessageDTO();
                payload.setCommandId(command.getId());
                payload.setCommandLogId(command.getId());
                payload.setTenantId(device.getTenantId());
                payload.setProductId(device.getProductId());
                payload.setProductKey(device.getProductKey());
                payload.setDeviceId(device.getId());
                payload.setDeviceCode(device.getDeviceCode());
                payload.setGatewayNodeId(product == null ? "" : Strings.sBlank(product.getGatewayNodeId(), ""));
                payload.setNetworkProtocol(product == null || product.getNetworkProtocol() == null ? "" : product.getNetworkProtocol().getValue());
                payload.setCommandCode(command.getCommandCode());
                payload.setPayload(downlinkPayload);
                payload.setPayloadJson(Strings.sBlank(command.getPayloadJson(), "{}"));
                payload.setReplyRequired(command.isReplyRequired());
                payload.setDeadlineAt(command.getDeadlineAt());
                DeviceMessagePublishResultDTO publishResult = deviceMessageTemplate.publishDownlinkCommand(payload, "handler");
                command.setMessageId(publishResult.getMessageId());
                command.setMessageTopic(publishResult.getTopic());
                command.setQueuedAt(publishResult.getPublishedAt());
                command.setUpdatedBy("handler");
                dao.updateIgnoreNull(command);
            } catch (Exception e) {
                log.warn("Handler 派发待下发指令失败: tenantId={}, deviceId={}, commandId={}, error={}",
                        device.getTenantId(), device.getId(), command.getId(), Strings.sBlank(e.getMessage(), "unknown"));
            }
        }
    }

    private boolean isExpired(Device_command command, long now) {
        return command != null && command.getDeadlineAt() != null && command.getDeadlineAt() > 0L && command.getDeadlineAt() <= now;
    }

    private String resolvePendingDownlinkPayload(Device_command command, Device_info device, Device_protocol protocol) {
        if (protocol == null) {
            return Strings.sBlank(command.getPayloadJson(), "{}");
        }
        return scriptExecutionService.encode(protocol, command.getCommandCode(), command.getPayloadJson(), device);
    }

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }

    private String buildDataMessageId(String sourceMessageId, String identifier) {
        String base = Strings.isNotBlank(sourceMessageId) ? sourceMessageId : String.valueOf(System.currentTimeMillis());
        return base + "_" + sanitize(Strings.sBlank(identifier, "raw"));
    }

    private void publishRawArchive(DeviceMessageEnvelope<DeviceRawMessageDTO> source,
                                   String protocol,
                                   String parsedJson,
                                   boolean success,
                                   long deviceAt) {
        if (!relationalRawEnabled || source.getPayload() == null) {
            return;
        }
        DeviceRawMessageDTO payload = new DeviceRawMessageDTO();
        payload.setProtocol(Strings.sBlank(protocol, source.getPayload().getProtocol()));
        payload.setEndpoint(source.getPayload().getEndpoint());
        payload.setSourceIp(source.getPayload().getSourceIp());
        payload.setPayload(source.getPayload().getPayload());
        payload.setParsedJson(parsedJson);
        payload.setReceivedAt(deviceAt);

        DeviceMessageEnvelope<DeviceRawMessageDTO> archiveEnvelope = new DeviceMessageEnvelope<>();
        archiveEnvelope.setMessageId(source.getMessageId());
        archiveEnvelope.setTenantId(source.getTenantId());
        archiveEnvelope.setProductId(source.getProductId());
        archiveEnvelope.setProductKey(source.getProductKey());
        archiveEnvelope.setDeviceId(source.getDeviceId());
        archiveEnvelope.setDeviceCode(source.getDeviceCode());
        archiveEnvelope.setGatewayNodeId(source.getGatewayNodeId());
        archiveEnvelope.setScene(DeviceMessageScene.RAW_UPLINK);
        archiveEnvelope.setPattern(DeviceMessagePattern.TOPIC);
        archiveEnvelope.setTopic(rawArchiveTopic);
        archiveEnvelope.setOccurredAt(deviceAt);
        archiveEnvelope.setRoutingKey(source.getDeviceCode());
        archiveEnvelope.getHeaders().putAll(source.getHeaders());
        archiveEnvelope.getHeaders().put("archiveSuccess", String.valueOf(success));
        archiveEnvelope.setPayload(payload);
        deviceMessageTemplate.publish(archiveEnvelope, Strings.sBlank(source.getMessageId(), source.getDeviceId()), "handler");
        log.info("Handler 发布原始归档消息: topic={}, messageId={}, productKey={}, deviceCode={}, success={}",
                archiveEnvelope.getTopic(), archiveEnvelope.getMessageId(), archiveEnvelope.getProductKey(), archiveEnvelope.getDeviceCode(), success);
    }

    private DeviceEventLevel parseEventLevel(String level) {
        if (Strings.isBlank(level)) return DeviceEventLevel.INFO;
        try {
            return DeviceEventLevel.valueOf(level.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return DeviceEventLevel.INFO;
        }
    }

    private DeviceEventSourceType parseSourceType(String sourceType) {
        if (Strings.isBlank(sourceType)) return DeviceEventSourceType.DEVICE;
        try {
            return DeviceEventSourceType.valueOf(sourceType.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return DeviceEventSourceType.DEVICE;
        }
    }

    private String runtimeKey(String tenantId, String deviceId) {
        return "wk:device:runtime:" + tenantId + ":" + deviceId;
    }
}
