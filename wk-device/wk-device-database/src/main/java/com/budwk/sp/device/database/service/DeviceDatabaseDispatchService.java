package com.budwk.sp.device.database.service;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import com.budwk.sp.device.dto.*;
import com.budwk.sp.device.enums.DeviceMessageDirection;
import com.budwk.sp.device.enums.DeviceMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class DeviceDatabaseDispatchService {
    private final DeviceDatabaseProperties properties;
    private final ObjectMapper objectMapper;
    private final DefaultArchiveService postgresArchiveService;
    private final TdengineArchiveService tdengineArchiveService;
    private final ObjectProvider<MongoMirrorService> mongoMirrorServiceProvider;
    private final DatabaseCommandStatusService commandStatusService;

    public DeviceDatabaseDispatchService(DeviceDatabaseProperties properties,
                                         ObjectMapper objectMapper,
                                         DefaultArchiveService postgresArchiveService,
                                         TdengineArchiveService tdengineArchiveService,
                                         ObjectProvider<MongoMirrorService> mongoMirrorServiceProvider,
                                         DatabaseCommandStatusService commandStatusService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.postgresArchiveService = postgresArchiveService;
        this.tdengineArchiveService = tdengineArchiveService;
        this.mongoMirrorServiceProvider = mongoMirrorServiceProvider;
        this.commandStatusService = commandStatusService;
    }

    public void handleRaw(DeviceMessageEnvelope<DeviceRawMessageDTO> envelope) {
        DeviceRawMessageDTO payload = envelope.getPayload();
        if (payload == null) return;
        long ts = payload.getReceivedAt() == null ? System.currentTimeMillis() : payload.getReceivedAt();
        boolean success = readArchiveSuccess(envelope);
        switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoArchiveRaw(buildDocument(envelope, ts, rawPayloadMap(payload, envelope, ts, DeviceMessageDirection.U.getValue(), DeviceMessageType.RAW.getValue(), success)));
            case TDENGINE -> tdengineArchiveService.enqueueRaw(envelope.getProductKey(), ts, envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    Strings.sBlank(envelope.getMessageId(), ""), DeviceMessageDirection.U.getValue(), DeviceMessageType.RAW.getValue(), payload.getProtocol(), envelope.getTopic(), envelope.getGatewayNodeId(), payload.getPayload(), Strings.sBlank(payload.getParsedJson(), ""), success);
            default -> {
                if (Strings.sBlank(envelope.getTopic(), "").equals(properties.getRawArchiveTopic())) {
                    log.info("Database 开始归档原始报文: topic={}, messageId={}, productKey={}, deviceCode={}, success={}",
                            envelope.getTopic(), envelope.getMessageId(), envelope.getProductKey(), envelope.getDeviceCode(), success);
                    postgresArchiveService.archiveRaw(envelope.getProductKey(), envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                            Strings.sBlank(envelope.getMessageId(), ""), DeviceMessageDirection.U.getValue(), DeviceMessageType.RAW.getValue(), payload.getProtocol(), envelope.getTopic(), envelope.getGatewayNodeId(), payload.getPayload(), Strings.sBlank(payload.getParsedJson(), ""), success, ts);
                }
            }
        }
        completePendingCommand(envelope, ts, rawCommandReplyMap(payload, envelope, ts));
    }

    public void handleDownlink(DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope) {
        DeviceDownlinkCommandMessageDTO payload = envelope.getPayload();
        if (payload == null) return;
        long ts = envelope.getOccurredAt() == null ? System.currentTimeMillis() : envelope.getOccurredAt();
        switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoArchiveRaw(buildDocument(envelope, ts, rawPayloadMap(payload, envelope, ts, DeviceMessageDirection.D.getValue(), DeviceMessageType.COMMAND.getValue())));
            case TDENGINE -> tdengineArchiveService.enqueueRaw(envelope.getProductKey(), ts, envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    Strings.sBlank(envelope.getMessageId(), ""), DeviceMessageDirection.D.getValue(), DeviceMessageType.COMMAND.getValue(), payload.getNetworkProtocol(), envelope.getTopic(), envelope.getGatewayNodeId(), Strings.sBlank(payload.getPayload(), payload.getPayloadJson()), "", true);
            default -> postgresArchiveService.archiveRaw(envelope.getProductKey(), envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    Strings.sBlank(envelope.getMessageId(), ""), DeviceMessageDirection.D.getValue(), DeviceMessageType.COMMAND.getValue(), payload.getNetworkProtocol(), envelope.getTopic(), envelope.getGatewayNodeId(), Strings.sBlank(payload.getPayload(), payload.getPayloadJson()), "", true, ts);
        }
    }

    public void handleNormalized(DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope) {
        DeviceUplinkMessageDTO payload = envelope.getPayload();
        if (payload == null) return;
        long ts = payload.getDeviceAt() == null ? System.currentTimeMillis() : payload.getDeviceAt();
        String sourceMessageId = readSourceMessageId(envelope);
        switch (properties.resolveDataStorage()) {
            case MONGODB -> mongoArchiveData(buildDocument(envelope, ts, normalizedPayloadMap(payload, ts)));
            case TDENGINE -> tdengineArchiveService.enqueueData(envelope.getProductKey(), ts, envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    sourceMessageId, Strings.sBlank(payload.getIdentifier(), payload.getMessageType()), Strings.sBlank(readMetadataName(payload.getMetadataJson()), payload.getIdentifier()), payload.getDataJson(), readMetadataUnit(payload.getMetadataJson()));
            default -> postgresArchiveService.archiveData(envelope.getProductKey(), envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    sourceMessageId, Strings.sBlank(payload.getIdentifier(), payload.getMessageType()), payload.getDataJson(), ts);
        }
        completePendingCommand(envelope, ts, normalizedCommandReplyMap(payload, envelope, ts));
    }

    public void handleEvent(DeviceMessageEnvelope<DeviceEventMessageDTO> envelope) {
        DeviceEventMessageDTO payload = envelope.getPayload();
        if (payload == null) return;
        long ts = payload.getDeviceAt() == null ? System.currentTimeMillis() : payload.getDeviceAt();
        switch (properties.resolveEventStorage()) {
            case MONGODB -> mongoArchiveEvent(buildDocument(envelope, ts, eventPayloadMap(payload, ts)));
            case TDENGINE -> tdengineArchiveService.enqueueEvent(envelope.getProductKey(), ts, envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    payload.getEventCode(), payload.getEventName(), payload.getLevel(), payload.getSourceType(), payload.getContentJson());
            default -> postgresArchiveService.archiveEvent(envelope.getProductKey(), envelope.getTenantId(), envelope.getProductId(), envelope.getDeviceId(), envelope.getDeviceCode(),
                    payload.getEventCode(), payload.getEventName(), payload.getLevel(), payload.getSourceType(), payload.getContentJson(), ts);
        }
        completePendingCommand(envelope, ts, eventCommandReplyMap(payload, envelope, ts));
    }

    private Map<String, Object> rawPayloadMap(DeviceRawMessageDTO payload,
                                              DeviceMessageEnvelope<?> envelope,
                                              long ts,
                                              String direction,
                                              String messageType,
                                              boolean success) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("direction", direction);
        map.put("messageType", messageType);
        map.put("protocol", Strings.sBlank(payload.getProtocol(), ""));
        map.put("topic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("sourceIp", Strings.sBlank(payload.getSourceIp(), ""));
        map.put("endpoint", Strings.sBlank(payload.getEndpoint(), ""));
        map.put("payload", Strings.sBlank(payload.getPayload(), ""));
        map.put("parsedJson", Strings.sBlank(payload.getParsedJson(), ""));
        map.put("gatewayNodeId", Strings.sBlank(envelope.getGatewayNodeId(), ""));
        map.put("occurredAt", ts);
        map.put("deviceAt", ts);
        map.put("success", success);
        return map;
    }

    private Map<String, Object> rawPayloadMap(DeviceDownlinkCommandMessageDTO payload,
                                              DeviceMessageEnvelope<?> envelope,
                                              long ts,
                                              String direction,
                                              String messageType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("direction", direction);
        map.put("messageType", messageType);
        map.put("protocol", Strings.sBlank(payload.getNetworkProtocol(), ""));
        map.put("topic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("commandCode", Strings.sBlank(payload.getCommandCode(), ""));
        map.put("payload", Strings.sBlank(payload.getPayload(), payload.getPayloadJson()));
        map.put("gatewayNodeId", Strings.sBlank(envelope.getGatewayNodeId(), ""));
        map.put("occurredAt", ts);
        map.put("deviceAt", ts);
        map.put("success", true);
        return map;
    }

    private Map<String, Object> normalizedPayloadMap(DeviceUplinkMessageDTO payload, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("identifier", Strings.sBlank(payload.getIdentifier(), payload.getMessageType()));
        map.put("name", Strings.sBlank(readMetadataName(payload.getMetadataJson()), payload.getIdentifier()));
        map.put("messageType", Strings.sBlank(payload.getMessageType(), ""));
        map.put("dataJson", Strings.sBlank(payload.getDataJson(), "{}"));
        map.put("metadataJson", Strings.sBlank(payload.getMetadataJson(), "{}"));
        map.put("unit", readMetadataUnit(payload.getMetadataJson()));
        map.put("occurredAt", ts);
        map.put("deviceAt", ts);
        return map;
    }

    private Map<String, Object> eventPayloadMap(DeviceEventMessageDTO payload, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventCode", Strings.sBlank(payload.getEventCode(), ""));
        map.put("eventName", Strings.sBlank(payload.getEventName(), ""));
        map.put("level", Strings.sBlank(payload.getLevel(), ""));
        map.put("sourceType", Strings.sBlank(payload.getSourceType(), ""));
        map.put("contentJson", Strings.sBlank(payload.getContentJson(), "{}"));
        map.put("occurredAt", ts);
        map.put("deviceAt", ts);
        map.put("handled", false);
        return map;
    }

    private Map<String, Object> commandPayloadMap(DeviceDownlinkCommandMessageDTO payload,
                                                  DeviceMessageEnvelope<?> envelope,
                                                  long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("commandId", Strings.sBlank(payload.getCommandId(), ""));
        map.put("commandCode", Strings.sBlank(payload.getCommandCode(), ""));
        map.put("payloadJson", Strings.sBlank(payload.getPayloadJson(), "{}"));
        map.put("replyRequired", payload.isReplyRequired());
        map.put("deadlineAt", payload.getDeadlineAt());
        map.put("messageId", Strings.sBlank(envelope.getMessageId(), ""));
        map.put("messageTopic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("queuedAt", ts);
        map.put("finishedAt", null);
        map.put("responseJson", "");
        map.put("errorMessage", "");
        map.put("status", "PENDING");
        map.put("occurredAt", ts);
        return map;
    }

    private void mongoArchiveRaw(Document document) {
        MongoMirrorService service = mongoMirrorServiceProvider.getIfAvailable();
        if (service != null) {
            service.archiveRaw(document);
        }
    }

    private void mongoArchiveData(Document document) {
        MongoMirrorService service = mongoMirrorServiceProvider.getIfAvailable();
        if (service != null) {
            service.archiveData(document);
        }
    }

    private void mongoArchiveEvent(Document document) {
        MongoMirrorService service = mongoMirrorServiceProvider.getIfAvailable();
        if (service != null) {
            service.archiveEvent(document);
        }
    }

    private void mongoArchiveCommand(Document document) {
        MongoMirrorService service = mongoMirrorServiceProvider.getIfAvailable();
        if (service != null) {
            service.archiveCommand(document);
        }
    }

    private Document buildDocument(DeviceMessageEnvelope<?> envelope, long occurredAt, Map<String, Object> payloadMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tenantId", envelope.getTenantId());
        map.put("productId", envelope.getProductId());
        map.put("productKey", envelope.getProductKey());
        map.put("deviceId", envelope.getDeviceId());
        map.put("deviceCode", envelope.getDeviceCode());
        map.put("gatewayNodeId", Strings.sBlank(envelope.getGatewayNodeId(), ""));
        map.put("messageId", Strings.sBlank(envelope.getMessageId(), ""));
        map.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        map.put("createdAt", occurredAt);
        map.put("updatedAt", occurredAt);
        map.put("delFlag", false);
        map.putAll(payloadMap);
        return new Document(map);
    }

    private String readMetadataName(String metadataJson) {
        return readMetadataField(metadataJson, "name");
    }

    private String readMetadataUnit(String metadataJson) {
        return readMetadataField(metadataJson, "unit");
    }

    private String readMetadataField(String metadataJson, String field) {
        if (Strings.isBlank(metadataJson)) {
            return "";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(metadataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            return Strings.sNull(String.valueOf(map.getOrDefault(field, ""))).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean readArchiveSuccess(DeviceMessageEnvelope<?> envelope) {
        return !"false".equalsIgnoreCase(Strings.sBlank(envelope.getHeaders().get("archiveSuccess"), "true"));
    }

    private String readSourceMessageId(DeviceMessageEnvelope<?> envelope) {
        return Strings.sBlank(envelope.getHeaders().get("sourceMessageId"), Strings.sBlank(envelope.getMessageId(), ""));
    }

    private void completePendingCommand(DeviceMessageEnvelope<?> envelope, long finishedAt, Map<String, Object> replyPayload) {
        String messageId = readSourceMessageId(envelope);
        if (Strings.isBlank(messageId)) {
            return;
        }
        commandStatusService.completeByMessageId(envelope.getTenantId(), envelope.getProductKey(), messageId, finishedAt, writeJson(replyPayload), "database");
    }

    private Map<String, Object> rawCommandReplyMap(DeviceRawMessageDTO payload, DeviceMessageEnvelope<?> envelope, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        map.put("topic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("protocol", Strings.sBlank(payload.getProtocol(), ""));
        map.put("payload", Strings.sBlank(payload.getPayload(), ""));
        map.put("parsedJson", Strings.sBlank(payload.getParsedJson(), ""));
        map.put("receivedAt", ts);
        return map;
    }

    private Map<String, Object> normalizedCommandReplyMap(DeviceUplinkMessageDTO payload, DeviceMessageEnvelope<?> envelope, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        map.put("topic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("identifier", Strings.sBlank(payload.getIdentifier(), ""));
        map.put("messageType", Strings.sBlank(payload.getMessageType(), ""));
        map.put("dataJson", Strings.sBlank(payload.getDataJson(), "{}"));
        map.put("metadataJson", Strings.sBlank(payload.getMetadataJson(), "{}"));
        map.put("deviceAt", ts);
        return map;
    }

    private Map<String, Object> eventCommandReplyMap(DeviceEventMessageDTO payload, DeviceMessageEnvelope<?> envelope, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        map.put("topic", Strings.sBlank(envelope.getTopic(), ""));
        map.put("eventCode", Strings.sBlank(payload.getEventCode(), ""));
        map.put("eventName", Strings.sBlank(payload.getEventName(), ""));
        map.put("level", Strings.sBlank(payload.getLevel(), ""));
        map.put("sourceType", Strings.sBlank(payload.getSourceType(), ""));
        map.put("contentJson", Strings.sBlank(payload.getContentJson(), "{}"));
        map.put("deviceAt", ts);
        return map;
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
