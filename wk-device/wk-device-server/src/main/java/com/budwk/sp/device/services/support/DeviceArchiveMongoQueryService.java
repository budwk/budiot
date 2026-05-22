package com.budwk.sp.device.services.support;

import com.budwk.sp.device.config.DeviceArchiveProperties;
import com.budwk.sp.device.entity.Device_command_log;
import com.budwk.sp.device.entity.Device_data_log;
import com.budwk.sp.device.entity.Device_event_log;
import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.enums.DeviceEventLevel;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.device.enums.DeviceMessageDirection;
import com.budwk.sp.device.enums.DeviceMessageType;
import org.bson.Document;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DeviceArchiveMongoQueryService {
    private final DeviceArchiveProperties properties;
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    public DeviceArchiveMongoQueryService(DeviceArchiveProperties properties, ObjectProvider<MongoTemplate> mongoTemplateProvider) {
        this.properties = properties;
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listRawLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_raw_log> logs = new ArrayList<>();
        Query query = buildDeviceQuery(tenantId, deviceId, startAt, endAt, "deviceAt")
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        for (String collection : rawCollections(productKey)) {
            logs.addAll(mongo().find(query, Document.class, collection).stream().map(this::mapRaw).toList());
        }
        return sortAndLimit(logs, limit);
    }

    public List<Device_raw_log> listRawLogsByRange(String tenantId, long startAt, long endAt) {
        List<Device_raw_log> logs = new ArrayList<>();
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("tenantId").is(tenantId),
                timeCriteria(startAt, endAt)
        ));
        for (String collection : rawCollections(null)) {
            logs.addAll(mongo().find(query, Document.class, collection).stream().map(this::mapRaw).toList());
        }
        return sortAndLimit(logs, Integer.MAX_VALUE);
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listDataLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_data_log> logs = new ArrayList<>();
        Query query = buildDeviceQuery(tenantId, deviceId, startAt, endAt, "deviceAt")
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        for (String collection : collections("data", productKey)) {
            logs.addAll(mongo().find(query, Document.class, collection).stream().map(this::mapData).toList());
        }
        return sortAndLimit(logs, limit);
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listEventLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_event_log> logs = new ArrayList<>();
        Query query = buildDeviceQuery(tenantId, deviceId, startAt, endAt, "deviceAt")
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        for (String collection : collections("event", productKey)) {
            logs.addAll(mongo().find(query, Document.class, collection).stream().map(this::mapEvent).toList());
        }
        return sortAndLimit(logs, limit);
    }

    public List<Device_command_log> listCommandLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listCommandLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_command_log> listCommandLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_command_log> logs = new ArrayList<>();
        Query query = buildDeviceQuery(tenantId, deviceId, startAt, endAt, "createdAt")
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        for (String collection : collections("command", productKey)) {
            logs.addAll(mongo().find(query, Document.class, collection).stream().map(this::mapCommand).toList());
        }
        return sortAndLimit(logs, limit);
    }

    public long countRawLogs(String tenantId, long startAt, long endAt) {
        long total = 0L;
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("tenantId").is(tenantId),
                timeCriteria(startAt, endAt)
        ));
        for (String collection : rawCollections(null)) {
            total += mongo().count(query, collection);
        }
        return total;
    }

    public long countHistoricalActiveDevices(String tenantId, long startAt, long endAt) {
        Set<String> deviceIds = new LinkedHashSet<>();
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("tenantId").is(tenantId),
                timeCriteria(startAt, endAt)
        ));
        query.fields().include("deviceId");
        for (String collection : rawCollections(null)) {
            mongo().find(query, Document.class, collection).forEach(doc -> deviceIds.add(readString(doc, "deviceId")));
        }
        return deviceIds.size();
    }

    public long countEventLogs(String tenantId, long startAt, long endAt, List<DeviceEventSourceType> sourceTypes) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("tenantId").is(tenantId),
                timeCriteria(startAt, endAt)
        );
        Query query = new Query(criteria);
        if (sourceTypes != null && !sourceTypes.isEmpty()) {
            query.addCriteria(Criteria.where("sourceType").in(sourceTypes.stream().map(DeviceEventSourceType::getValue).toList()));
        }
        long total = 0L;
        for (String collection : collections("event", null)) {
            total += mongo().count(query, collection);
        }
        return total;
    }

    private List<String> rawCollections(String productKey) {
        return collections(List.of("raw", "message"), productKey);
    }

    private List<String> collections(String category, String productKey) {
        return collections(List.of(category), productKey);
    }

    private List<String> collections(List<String> categories, String productKey) {
        MongoTemplate mongoTemplate = mongo();
        if (mongoTemplate == null || !properties.isMongoEnabled()) {
            return List.of();
        }
        String prefix = properties.getMongoCollectionPrefix() + "_";
        Set<String> allNames = mongoTemplate.getCollectionNames();
        Set<String> matched = new LinkedHashSet<>();
        for (String category : categories) {
            if (Strings.isNotBlank(productKey)) {
                String expected = prefix + category + "_" + sanitize(productKey);
                if (allNames.contains(expected)) {
                    matched.add(expected);
                }
                continue;
            }
            String categoryPrefix = prefix + category + "_";
            for (String name : allNames) {
                if (name.startsWith(categoryPrefix)) {
                    matched.add(name);
                }
            }
        }
        return new ArrayList<>(matched);
    }

    private MongoTemplate mongo() {
        return mongoTemplateProvider.getIfAvailable();
    }

    private Query buildDeviceQuery(String tenantId, String deviceId, Long startAt, Long endAt, String timeField) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("tenantId").is(tenantId));
        criteria.add(Criteria.where("deviceId").is(deviceId));
        if (startAt != null || endAt != null) {
            criteria.add(timeCriteria(startAt, endAt, timeField));
        }
        return new Query(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
    }

    private Criteria timeCriteria(long startAt, long endAt) {
        return new Criteria().orOperator(
                Criteria.where("createdAt").gte(startAt).lte(endAt),
                new Criteria().andOperator(Criteria.where("createdAt").exists(false), Criteria.where("occurredAt").gte(startAt).lte(endAt))
        );
    }

    private Criteria timeCriteria(Long startAt, Long endAt, String field) {
        List<Criteria> criteria = new ArrayList<>();
        if (startAt != null) {
            criteria.add(Criteria.where(field).gte(startAt));
        }
        if (endAt != null) {
            criteria.add(Criteria.where(field).lte(endAt));
        }
        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }

    private Device_raw_log mapRaw(Document doc) {
        Device_raw_log log = new Device_raw_log();
        fillBase(log, doc);
        log.setId(readId(doc));
        log.setTenantId(readString(doc, "tenantId"));
        log.setDeviceId(readString(doc, "deviceId"));
        log.setDeviceCode(readString(doc, "deviceCode"));
        log.setProductId(readString(doc, "productId"));
        log.setMessageId(readString(doc, "messageId"));
        log.setDirection(parseDirection(readString(doc, "direction")));
        log.setMessageType(parseMessageType(readString(doc, "messageType")));
        log.setProtocol(readString(doc, "protocol"));
        log.setTopic(readString(doc, "topic"));
        log.setGatewayNodeId(readString(doc, "gatewayNodeId"));
        log.setPayload(readString(doc, "payload"));
        log.setParsedJson(readString(doc, "parsedJson"));
        log.setSuccess(readBoolean(doc, "success"));
        log.setDeviceAt(readLong(doc, "deviceAt", "occurredAt"));
        return log;
    }

    private Device_data_log mapData(Document doc) {
        Device_data_log log = new Device_data_log();
        fillBase(log, doc);
        log.setId(readId(doc));
        log.setTenantId(readString(doc, "tenantId"));
        log.setDeviceId(readString(doc, "deviceId"));
        log.setDeviceCode(readString(doc, "deviceCode"));
        log.setProductId(readString(doc, "productId"));
        log.setMessageId(readString(doc, "messageId"));
        log.setIdentifier(readString(doc, "identifier"));
        log.setName(readString(doc, "name"));
        log.setValueJson(readString(doc, "dataJson", "valueJson"));
        log.setUnit(readMetadataUnit(doc));
        log.setDeviceAt(readLong(doc, "deviceAt", "occurredAt"));
        return log;
    }

    private Device_event_log mapEvent(Document doc) {
        Device_event_log log = new Device_event_log();
        fillBase(log, doc);
        log.setId(readId(doc));
        log.setTenantId(readString(doc, "tenantId"));
        log.setDeviceId(readString(doc, "deviceId"));
        log.setDeviceCode(readString(doc, "deviceCode"));
        log.setProductId(readString(doc, "productId"));
        log.setEventCode(readString(doc, "eventCode"));
        log.setEventName(readString(doc, "eventName"));
        log.setLevel(parseEventLevel(readString(doc, "level")));
        log.setSourceType(parseEventSource(readString(doc, "sourceType")));
        log.setContentJson(readString(doc, "contentJson"));
        log.setHandled(readBoolean(doc, "handled"));
        log.setDeviceAt(readLong(doc, "deviceAt", "occurredAt"));
        return log;
    }

    private Device_command_log mapCommand(Document doc) {
        Device_command_log log = new Device_command_log();
        fillBase(log, doc);
        log.setId(readId(doc));
        log.setTenantId(readString(doc, "tenantId"));
        log.setDeviceId(readString(doc, "deviceId"));
        log.setDeviceCode(readString(doc, "deviceCode"));
        log.setProductId(readString(doc, "productId"));
        log.setCommandCode(readString(doc, "commandCode"));
        log.setPayloadJson(readString(doc, "payloadJson"));
        log.setReplyRequired(readBoolean(doc, "replyRequired"));
        log.setDeadlineAt(readLong(doc, "deadlineAt"));
        log.setMessageId(readString(doc, "messageId"));
        log.setMessageTopic(readString(doc, "messageTopic"));
        log.setQueuedAt(readLong(doc, "queuedAt"));
        log.setFinishedAt(readLong(doc, "finishedAt"));
        log.setResponseJson(readString(doc, "responseJson"));
        log.setErrorMessage(readString(doc, "errorMessage"));
        log.setStatus(parseCommandStatus(readString(doc, "status")));
        return log;
    }

    private <T extends com.budwk.sp.starter.database.entity.BaseEntity> List<T> sortAndLimit(List<T> list, int limit) {
        return list.stream()
                .sorted(Comparator.comparing(item -> item.getCreatedAt() == null ? 0L : item.getCreatedAt(), Comparator.reverseOrder()))
                .limit(Math.max(limit, 1))
                .toList();
    }

    private void fillBase(com.budwk.sp.starter.database.entity.BaseEntity entity, Document doc) {
        entity.setCreatedBy(readString(doc, "createdBy"));
        entity.setCreatedAt(readLong(doc, "createdAt", "occurredAt"));
        entity.setUpdatedBy(readString(doc, "updatedBy"));
        entity.setUpdatedAt(readLong(doc, "updatedAt", "occurredAt"));
        entity.setDelFlag(readBoolean(doc, "delFlag"));
    }

    private String readId(Document doc) {
        Object id = doc.get("id");
        if (id != null) {
            return String.valueOf(id);
        }
        Object objectId = doc.get("_id");
        return objectId == null ? "" : String.valueOf(objectId);
    }

    private String readString(Document doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private Long readLong(Document doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    private boolean readBoolean(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String readMetadataUnit(Document doc) {
        String unit = readString(doc, "unit");
        if (Strings.isNotBlank(unit)) {
            return unit;
        }
        return "";
    }

    private DeviceMessageDirection parseDirection(String value) {
        for (DeviceMessageDirection item : DeviceMessageDirection.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sBlank(value, ""))) {
                return item;
            }
        }
        return null;
    }

    private DeviceMessageType parseMessageType(String value) {
        for (DeviceMessageType item : DeviceMessageType.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sBlank(value, ""))) {
                return item;
            }
        }
        return null;
    }

    private DeviceEventLevel parseEventLevel(String value) {
        for (DeviceEventLevel item : DeviceEventLevel.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sBlank(value, ""))) {
                return item;
            }
        }
        return null;
    }

    private DeviceEventSourceType parseEventSource(String value) {
        for (DeviceEventSourceType item : DeviceEventSourceType.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sBlank(value, ""))) {
                return item;
            }
        }
        return null;
    }

    private DeviceCommandStatus parseCommandStatus(String value) {
        for (DeviceCommandStatus item : DeviceCommandStatus.values()) {
            if (item.name().equalsIgnoreCase(Strings.sBlank(value, ""))) {
                return item;
            }
        }
        return null;
    }

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }
}
