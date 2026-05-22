package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.enums.DeviceArchiveStorageType;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.handler.config.HandlerArchiveProperties;
import com.budwk.sp.device.support.DeviceCommandSchemaService;
import com.budwk.sp.starter.database.idgen.IdService;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.TimeSeriesGranularity;
import com.mongodb.client.model.TimeSeriesOptions;
import org.bson.Document;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class HandlerCommandArchiveService {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String TIME_FIELD = "ts";
    private static final String META_FIELD = "meta";
    private final Dao dao;
    private final IdService idService;
    private final HandlerArchiveProperties properties;
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final DeviceCommandSchemaService schemaService;
    private final Set<String> mongoCollections = ConcurrentHashMap.newKeySet();

    public HandlerCommandArchiveService(Dao dao,
                                        IdService idService,
                                        HandlerArchiveProperties properties,
                                        ObjectProvider<MongoTemplate> mongoTemplateProvider,
                                        DeviceCommandSchemaService schemaService) {
        this.dao = dao;
        this.idService = idService;
        this.properties = properties;
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.schemaService = schemaService;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void expirePendingCommand(Device_command command, Device_info device, long finishedAt) {
        if (command == null || device == null) {
            return;
        }
        if (properties.resolveCommandStorage() == DeviceArchiveStorageType.MONGODB) {
            archiveMongo(command, device, finishedAt);
        } else {
            archiveRelational(command, device, finishedAt);
        }
        dao.delete(Device_command.class, command.getId());
    }

    private void archiveRelational(Device_command command, Device_info device, long finishedAt) {
        String table = resolveTargetTable("device_command_log", device.getProductKey(), readCreatedAt(command, finishedAt));
        ensureTable("device_command_log", table);
        schemaService.ensureHistoryColumns(table);
        Sql sql = Sqls.create("INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, commandCode, payloadJson, replyRequired, deadlineAt, messageId, messageTopic, queuedAt, sentAt, finishedAt, responseJson, errorMessage, status, createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@commandCode,@payloadJson,@replyRequired,@deadlineAt,@messageId,@messageTopic,@queuedAt,@sentAt,@finishedAt,@responseJson,@errorMessage,@status,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)");
        sql.params().set("id", Strings.sBlank(command.getId(), idService.nextId()));
        sql.params().set("tenantId", command.getTenantId());
        sql.params().set("deviceId", command.getDeviceId());
        sql.params().set("deviceCode", command.getDeviceCode());
        sql.params().set("productId", command.getProductId());
        sql.params().set("commandCode", command.getCommandCode());
        sql.params().set("payloadJson", Strings.sBlank(command.getPayloadJson(), "{}"));
        sql.params().set("replyRequired", command.isReplyRequired());
        sql.params().set("deadlineAt", command.getDeadlineAt());
        sql.params().set("messageId", "");
        sql.params().set("messageTopic", "");
        sql.params().set("queuedAt", null);
        sql.params().set("sentAt", command.getSentAt());
        sql.params().set("finishedAt", finishedAt);
        sql.params().set("responseJson", "");
        sql.params().set("errorMessage", "指令超时");
        sql.params().set("status", DeviceCommandStatus.FAILED.name());
        sql.params().set("createdBy", Strings.sBlank(command.getCreatedBy(), "handler"));
        sql.params().set("createdAt", readCreatedAt(command, finishedAt));
        sql.params().set("updatedBy", "handler");
        sql.params().set("updatedAt", finishedAt);
        sql.params().set("delFlag", false);
        dao.execute(sql);
    }

    private void archiveMongo(Device_command command, Device_info device, long finishedAt) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            archiveRelational(command, device, finishedAt);
            return;
        }
        Document document = new Document();
        document.put("id", Strings.sBlank(command.getId(), idService.nextId()));
        document.put("tenantId", command.getTenantId());
        document.put("productId", command.getProductId());
        document.put("productKey", device.getProductKey());
        document.put("deviceId", command.getDeviceId());
        document.put("deviceCode", command.getDeviceCode());
        document.put("commandCode", command.getCommandCode());
        document.put("payloadJson", Strings.sBlank(command.getPayloadJson(), "{}"));
        document.put("replyRequired", command.isReplyRequired());
        document.put("deadlineAt", command.getDeadlineAt());
        document.put("messageId", "");
        document.put("messageTopic", "");
        document.put("queuedAt", null);
        document.put("sentAt", command.getSentAt());
        document.put("finishedAt", finishedAt);
        document.put("responseJson", "");
        document.put("errorMessage", "指令超时");
        document.put("status", DeviceCommandStatus.FAILED.name());
        document.put("createdBy", Strings.sBlank(command.getCreatedBy(), "handler"));
        document.put("createdAt", readCreatedAt(command, finishedAt));
        document.put("updatedBy", "handler");
        document.put("updatedAt", finishedAt);
        document.put("delFlag", false);
        long ts = readCreatedAt(command, finishedAt);
        document.put(TIME_FIELD, new Date(ts));
        document.put(META_FIELD, new Document()
                .append("tenantId", Strings.sBlank(command.getTenantId(), ""))
                .append("productId", Strings.sBlank(command.getProductId(), ""))
                .append("productKey", Strings.sBlank(device.getProductKey(), ""))
                .append("deviceId", Strings.sBlank(command.getDeviceId(), ""))
                .append("deviceCode", Strings.sBlank(command.getDeviceCode(), "")));
        String collection = properties.getMongoCollectionPrefix() + "_command_" + sanitize(device.getProductKey());
        ensureMongoCollection(mongoTemplate, collection);
        mongoTemplate.insert(document, collection);
    }

    private void ensureMongoCollection(MongoTemplate mongoTemplate, String collection) {
        if (!mongoCollections.add(collection)) {
            return;
        }
        Document info = mongoTemplate.getDb().listCollections().filter(new Document("name", collection)).first();
        if (info != null) {
            return;
        }
        CreateCollectionOptions options = new CreateCollectionOptions()
                .timeSeriesOptions(new TimeSeriesOptions(TIME_FIELD)
                        .metaField(META_FIELD)
                        .granularity(TimeSeriesGranularity.SECONDS));
        mongoTemplate.getDb().createCollection(collection, options);
    }

    private void ensureTable(String baseTable, String table) {
        if (baseTable.equals(table)) {
            return;
        }
        if (!tableExists(table)) {
            dao.execute(Sqls.create("CREATE TABLE IF NOT EXISTS " + table + " (LIKE " + baseTable + ")"));
        }
    }

    private boolean tableExists(String tableName) {
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> rs.next() ? rs.getString(1) : null);
        dao.execute(sql);
        return Strings.isNotBlank(sql.getString());
    }

    private String resolveTargetTable(String baseTable, String productKey, long at) {
        if ("device_command_log".equals(baseTable)) {
            YearMonth month = YearMonth.from(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
            return baseTable + "_" + sanitize(productKey) + "_" + MONTH_FORMATTER.format(month);
        }
        YearMonth month = YearMonth.from(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
        return baseTable + "_" + sanitize(productKey) + "_" + MONTH_FORMATTER.format(month);
    }

    private long readCreatedAt(Device_command command, long fallback) {
        return command.getCreatedAt() == null || command.getCreatedAt() <= 0L ? fallback : command.getCreatedAt();
    }

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }
}
