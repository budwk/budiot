package com.budwk.sp.device.database.service;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.support.DeviceThingPropertySupport;
import com.budwk.sp.starter.database.idgen.IdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class DefaultArchiveService {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private final Dao dao;
    private final DeviceDatabaseProperties properties;
    private final IdService idService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean warned = new AtomicBoolean(false);
    private volatile Boolean postgresDatabase;

    public DefaultArchiveService(Dao dao, DeviceDatabaseProperties properties, IdService idService, ObjectMapper objectMapper) {
        this.dao = dao;
        this.properties = properties;
        this.idService = idService;
        this.objectMapper = objectMapper;
    }

    public void archiveRaw(String productKey, String tenantId, String productId, String deviceId, String deviceCode,
                           String messageId, String direction, String messageType, String protocol, String topic, String gatewayNodeId,
                           String payload, String parsedJson, boolean success, long deviceAt) {
        String table = resolveTargetTable("device_raw_log", productKey, deviceAt);
        ensureTable("device_raw_log", table);
        long now = System.currentTimeMillis();
        if (Strings.isNotBlank(messageId) && updateExistingRaw(table, tenantId, messageId, direction, messageType, protocol, topic, gatewayNodeId, payload, parsedJson, success, deviceAt, now) > 0) {
            return;
        }
        String sqlText = "INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, messageId, direction, messageType, protocol, topic, gatewayNodeId, payload, parsedJson, success, deviceAt, createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@messageId,@direction,@messageType,@protocol,@topic,@gatewayNodeId,@payload,@parsedJson,@success,@deviceAt,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)";
        executeInsert(sqlText, sql -> {
            sql.params().set("id", id());
            sql.params().set("tenantId", tenantId);
            sql.params().set("deviceId", deviceId);
            sql.params().set("deviceCode", deviceCode);
            sql.params().set("productId", productId);
            sql.params().set("messageId", Strings.sBlank(messageId, ""));
            sql.params().set("direction", direction);
            sql.params().set("messageType", messageType);
            sql.params().set("protocol", protocol);
            sql.params().set("topic", topic);
            sql.params().set("gatewayNodeId", gatewayNodeId);
            sql.params().set("payload", payload);
            sql.params().set("parsedJson", Strings.sBlank(parsedJson, ""));
            sql.params().set("success", success);
            sql.params().set("deviceAt", deviceAt);
            sql.params().set("createdBy", "database");
            sql.params().set("createdAt", now);
            sql.params().set("updatedBy", "database");
            sql.params().set("updatedAt", now);
            sql.params().set("delFlag", false);
        }, table);
    }

    public void archiveData(String productKey, String tenantId, String productId, String deviceId, String deviceCode,
                            String messageId, String identifier, String valueJson, long deviceAt) {
        String table = resolveTargetTable("device_data_log", productKey, deviceAt);
        ensureTable("device_data_log", table);
        String columnName = ensureDataPropertyColumn(productId, table, identifier);
        if (Strings.isBlank(columnName)) {
            return;
        }
        String value = DeviceThingPropertySupport.normalizeValue(valueJson, objectMapper);
        long now = System.currentTimeMillis();
        if (Strings.isNotBlank(messageId) && updateExistingData(table, tenantId, messageId, columnName, value, deviceAt, now) > 0) {
            return;
        }
        String sqlText = "INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, messageId, deviceAt, " + columnName + ", createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@messageId,@deviceAt,@value,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)";
        executeInsert(sqlText, sql -> {
            sql.params().set("id", id());
            sql.params().set("tenantId", tenantId);
            sql.params().set("deviceId", deviceId);
            sql.params().set("deviceCode", deviceCode);
            sql.params().set("productId", productId);
            sql.params().set("messageId", Strings.sBlank(messageId, ""));
            sql.params().set("deviceAt", deviceAt);
            sql.params().set("value", value);
            sql.params().set("createdBy", "database");
            sql.params().set("createdAt", now);
            sql.params().set("updatedBy", "database");
            sql.params().set("updatedAt", now);
            sql.params().set("delFlag", false);
        }, table);
    }

    public void archiveEvent(String productKey, String tenantId, String productId, String deviceId, String deviceCode,
                             String eventCode, String eventName, String level, String sourceType, String contentJson, long deviceAt) {
        String table = resolveTargetTable("device_event_log", productKey, deviceAt);
        ensureTable("device_event_log", table);
        String sqlText = "INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, eventCode, eventName, level, sourceType, contentJson, handled, deviceAt, createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@eventCode,@eventName,@level,@sourceType,@contentJson,@handled,@deviceAt,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)";
        executeInsert(sqlText, sql -> {
            long now = System.currentTimeMillis();
            sql.params().set("id", id());
            sql.params().set("tenantId", tenantId);
            sql.params().set("deviceId", deviceId);
            sql.params().set("deviceCode", deviceCode);
            sql.params().set("productId", productId);
            sql.params().set("eventCode", eventCode);
            sql.params().set("eventName", eventName);
            sql.params().set("level", level);
            sql.params().set("sourceType", sourceType);
            sql.params().set("contentJson", contentJson);
            sql.params().set("handled", false);
            sql.params().set("deviceAt", deviceAt);
            sql.params().set("createdBy", "database");
            sql.params().set("createdAt", now);
            sql.params().set("updatedBy", "database");
            sql.params().set("updatedAt", now);
            sql.params().set("delFlag", false);
        }, table);
    }

    public void archiveCommand(String productKey, String tenantId, String productId, String deviceId, String deviceCode,
                               String commandCode, String payloadJson, boolean replyRequired, Long deadlineAt,
                               String messageId, String messageTopic, Long queuedAt, Long sentAt, Long finishedAt,
                               String responseJson, String errorMessage, String status, long createdAt) {
        String table = resolveTargetTable("device_command_log", productKey, createdAt);
        ensureTable("device_command_log", table);
        if (Strings.isNotBlank(messageId) && commandExists(table, tenantId, messageId)) {
            return;
        }
        String sqlText = "INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, commandCode, payloadJson, replyRequired, deadlineAt, messageId, messageTopic, queuedAt, sentAt, finishedAt, responseJson, errorMessage, status, createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@commandCode,@payloadJson,@replyRequired,@deadlineAt,@messageId,@messageTopic,@queuedAt,@sentAt,@finishedAt,@responseJson,@errorMessage,@status,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)";
        executeInsert(sqlText, sql -> {
            long now = System.currentTimeMillis();
            sql.params().set("id", id());
            sql.params().set("tenantId", tenantId);
            sql.params().set("deviceId", deviceId);
            sql.params().set("deviceCode", deviceCode);
            sql.params().set("productId", productId);
            sql.params().set("commandCode", commandCode);
            sql.params().set("payloadJson", payloadJson);
            sql.params().set("replyRequired", replyRequired);
            sql.params().set("deadlineAt", deadlineAt);
            sql.params().set("messageId", messageId);
            sql.params().set("messageTopic", messageTopic);
            sql.params().set("queuedAt", queuedAt);
            sql.params().set("sentAt", sentAt);
            sql.params().set("finishedAt", finishedAt);
            sql.params().set("responseJson", responseJson);
            sql.params().set("errorMessage", errorMessage);
            sql.params().set("status", status);
            sql.params().set("createdBy", "database");
            sql.params().set("createdAt", createdAt);
            sql.params().set("updatedBy", "database");
            sql.params().set("updatedAt", now);
            sql.params().set("delFlag", false);
        }, table);
    }

    @Scheduled(cron = "0 25 3 * * ?")
    public void cleanupExpired() {
        cleanupTableFamily("device_raw_log", properties.getMessageRetentionDays());
        cleanupTableFamily("device_data_log", properties.getDataRetentionDays());
        cleanupTableFamily("device_event_log", properties.getEventRetentionDays());
        cleanupTableFamily("device_command_log", properties.getCommandRetentionDays());
    }

    private void cleanupTableFamily(String baseTable, int retentionDays) {
        long expireAt = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L;
        try {
            if (tableExists(baseTable)) {
                deleteBefore(baseTable, expireAt);
            }
            for (String tableName : queryTableNames(baseTable + "_%")) {
                deleteBefore(tableName, expireAt);
                if (isTableEmpty(tableName)) {
                    dropTable(tableName);
                    log.info("Dropped empty partition table: {}", tableName);
                }
            }
        } catch (Exception e) {
            log.error("cleanup relational archive failed for {}", baseTable, e);
        }
    }

    private void deleteBefore(String tableName, long expireAt) {
        Sql sql = Sqls.create("DELETE FROM " + tableName + " WHERE createdAt < @expireAt");
        sql.params().set("expireAt", expireAt);
        dao.execute(sql);
    }

    private boolean isTableEmpty(String tableName) {
        Sql sql = Sqls.create("SELECT COUNT(*) FROM " + tableName);
        sql.setCallback((conn, rs, sql1) -> {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        });
        dao.execute(sql);
        Long count = (Long) sql.getResult();
        return count != null && count == 0L;
    }

    private void dropTable(String tableName) {
        Sql sql = Sqls.create("DROP TABLE IF EXISTS " + tableName);
        dao.execute(sql);
    }

    private boolean tableExists(String tableName) {
        List<String> tableNames = new ArrayList<>();
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
            return null;
        });
        dao.execute(sql);
        return !tableNames.isEmpty();
    }

    private List<String> queryTableNames(String likePattern) {
        List<String> tableNames = new ArrayList<>();
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name LIKE @pattern");
        sql.params().set("pattern", likePattern);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
            return null;
        });
        dao.execute(sql);
        return tableNames;
    }

    private void ensureTable(String baseTable, String table) {
        if (baseTable.equals(table)) {
            return;
        }
        if (!tableExists(table)) {
            Sql sql = Sqls.create("CREATE TABLE IF NOT EXISTS " + table + " (LIKE " + baseTable + ")");
            dao.execute(sql);
        }
        ensureCompatibleColumns(baseTable, table);
    }

    public int updateRawParsedJson(String productKey, String tenantId, String messageId, String parsedJson, long deviceAt) {
        if (Strings.isBlank(messageId)) {
            return 0;
        }
        String table = resolveTargetTable("device_raw_log", productKey, deviceAt);
        ensureTable("device_raw_log", table);
        Sql sql = Sqls.create("UPDATE " + table + " SET parsedJson=@parsedJson, updatedBy=@updatedBy, updatedAt=@updatedAt WHERE tenantId=@tenantId AND messageId=@messageId");
        sql.params().set("parsedJson", Strings.sBlank(parsedJson, ""));
        sql.params().set("updatedBy", "database");
        sql.params().set("updatedAt", System.currentTimeMillis());
        sql.params().set("tenantId", tenantId);
        sql.params().set("messageId", messageId);
        dao.execute(sql);
        return sql.getUpdateCount();
    }

    private void executeInsert(String sqlText, ParamSetter setter, String table) {
        try {
            Sql sql = Sqls.create(sqlText);
            setter.set(sql);
            dao.execute(sql);
        } catch (Exception e) {
            log.error("archive relational row failed: table={}", table, e);
        }
    }

    private int updateExistingRaw(String table, String tenantId, String messageId, String direction, String messageType,
                                  String protocol, String topic, String gatewayNodeId, String payload, String parsedJson,
                                  boolean success, long deviceAt, long now) {
        Sql sql = Sqls.create("UPDATE " + table + " SET direction=@direction, messageType=@messageType, protocol=@protocol, topic=@topic, gatewayNodeId=@gatewayNodeId, payload=@payload, success=@success, deviceAt=@deviceAt, updatedBy=@updatedBy, updatedAt=@updatedAt, parsedJson=CASE WHEN @parsedJson='' THEN parsedJson ELSE @parsedJson END WHERE tenantId=@tenantId AND messageId=@messageId");
        sql.params().set("direction", direction);
        sql.params().set("messageType", messageType);
        sql.params().set("protocol", protocol);
        sql.params().set("topic", topic);
        sql.params().set("gatewayNodeId", gatewayNodeId);
        sql.params().set("payload", payload);
        sql.params().set("success", success);
        sql.params().set("deviceAt", deviceAt);
        sql.params().set("updatedBy", "database");
        sql.params().set("updatedAt", now);
        sql.params().set("parsedJson", Strings.sBlank(parsedJson, ""));
        sql.params().set("tenantId", tenantId);
        sql.params().set("messageId", messageId);
        dao.execute(sql);
        return sql.getUpdateCount();
    }

    private int updateExistingData(String table, String tenantId, String messageId, String columnName,
                                   String value, long deviceAt, long now) {
        Sql sql = Sqls.create("UPDATE " + table + " SET " + columnName + "=@value, deviceAt=@deviceAt, updatedBy=@updatedBy, updatedAt=@updatedAt WHERE tenantId=@tenantId AND messageId=@messageId");
        sql.params().set("value", value);
        sql.params().set("deviceAt", deviceAt);
        sql.params().set("updatedBy", "database");
        sql.params().set("updatedAt", now);
        sql.params().set("tenantId", tenantId);
        sql.params().set("messageId", messageId);
        dao.execute(sql);
        return sql.getUpdateCount();
    }

    private boolean commandExists(String table, String tenantId, String messageId) {
        Sql sql = Sqls.create("SELECT 1 FROM " + table + " WHERE tenantId=@tenantId AND messageId=@messageId LIMIT 1");
        sql.params().set("tenantId", tenantId);
        sql.params().set("messageId", messageId);
        sql.setCallback((conn, rs, sql1) -> rs.next() ? 1 : null);
        dao.execute(sql);
        return sql.getInt() == 1;
    }

    private String resolveTargetTable(String baseTable, String productKey, long at) {
        if ("device_command_log".equals(baseTable)) {
            YearMonth month = YearMonth.from(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
            return baseTable + "_" + sanitize(productKey) + "_" + MONTH_FORMATTER.format(month);
        }
        YearMonth month = YearMonth.from(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
        return baseTable + "_" + sanitize(productKey) + "_" + MONTH_FORMATTER.format(month);
    }

    private void ensureCompatibleColumns(String baseTable, String tableName) {
        if ("device_raw_log".equals(baseTable)) {
            addColumnIfMissing(tableName, "messageId", "VARCHAR(80)");
            addColumnIfMissing(tableName, "parsedJson", "TEXT");
            return;
        }
        if ("device_data_log".equals(baseTable)) {
            dropColumnIfExists(tableName, "identifier");
            dropColumnIfExists(tableName, "name");
            dropColumnIfExists(tableName, "valueJson");
            dropColumnIfExists(tableName, "unit");
            addColumnIfMissing(tableName, "messageId", "VARCHAR(80)");
            return;
        }
        if ("device_command_log".equals(baseTable)) {
            addColumnIfMissing(tableName, "messageId", "VARCHAR(80)");
            addColumnIfMissing(tableName, "messageTopic", "VARCHAR(160)");
            addColumnIfMissing(tableName, "queuedAt", "BIGINT");
            addColumnIfMissing(tableName, "sentAt", "BIGINT");
            addColumnIfMissing(tableName, "finishedAt", "BIGINT");
            addColumnIfMissing(tableName, "responseJson", "TEXT");
            addColumnIfMissing(tableName, "errorMessage", "VARCHAR(255)");
            addColumnIfMissing(tableName, "status", "VARCHAR(20)");
        }
    }

    private String ensureDataPropertyColumn(String productId, String tableName, String identifier) {
        if (Strings.isBlank(identifier)) {
            return "";
        }
        Device_product product = dao.fetch(Device_product.class, Cnd.where("id", "=", productId).and("delFlag", "=", false));
        if (product != null) {
            for (Map<String, Object> property : DeviceThingPropertySupport.parseProperties(product.getThingPropertyJson(), objectMapper)) {
                String propertyIdentifier = DeviceThingPropertySupport.readIdentifier(property);
                if (Strings.isBlank(propertyIdentifier)) {
                    continue;
                }
                String propertyColumn = DeviceThingPropertySupport.toColumnName(propertyIdentifier);
                migrateLegacyDataColumn("device_data_log", DeviceThingPropertySupport.toLegacyColumnName(propertyIdentifier), propertyColumn);
                migrateLegacyDataColumn(tableName, DeviceThingPropertySupport.toLegacyColumnName(propertyIdentifier), propertyColumn);
                addColumnIfMissing("device_data_log", propertyColumn, "TEXT");
                addColumnIfMissing(tableName, propertyColumn, "TEXT");
            }
        }
        String targetColumn = DeviceThingPropertySupport.toColumnName(identifier);
        migrateLegacyDataColumn("device_data_log", DeviceThingPropertySupport.toLegacyColumnName(identifier), targetColumn);
        migrateLegacyDataColumn(tableName, DeviceThingPropertySupport.toLegacyColumnName(identifier), targetColumn);
        addColumnIfMissing("device_data_log", targetColumn, "TEXT");
        addColumnIfMissing(tableName, targetColumn, "TEXT");
        return targetColumn;
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (!tableExists(tableName)) {
            return;
        }
        Sql sql = Sqls.create("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + definition);
        dao.execute(sql);
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        if (!tableExists(tableName)) {
            return;
        }
        Sql sql = Sqls.create("ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + columnName);
        dao.execute(sql);
    }

    private void migrateLegacyDataColumn(String tableName, String legacyColumnName, String newColumnName) {
        if (legacyColumnName.equals(newColumnName) || !columnExists(tableName, legacyColumnName)) {
            return;
        }
        if (!columnExists(tableName, newColumnName)) {
            Sql rename = Sqls.create("ALTER TABLE " + tableName + " RENAME COLUMN " + legacyColumnName + " TO " + newColumnName);
            dao.execute(rename);
            return;
        }
        Sql copy = Sqls.create("UPDATE " + tableName + " SET " + newColumnName + "=" + legacyColumnName + " WHERE (" + newColumnName + " IS NULL OR " + newColumnName + "='') AND " + legacyColumnName + " IS NOT NULL");
        dao.execute(copy);
        dropColumnIfExists(tableName, legacyColumnName);
    }

    private boolean columnExists(String tableName, String columnName) {
        List<String> columnNames = new ArrayList<>();
        Sql sql = Sqls.create("SELECT column_name FROM information_schema.columns WHERE table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName AND column_name=@columnName");
        sql.params().set("tableName", tableName);
        sql.params().set("columnName", columnName);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                columnNames.add(rs.getString(1));
            }
            return null;
        });
        dao.execute(sql);
        return !columnNames.isEmpty();
    }

    private String sanitize(String value) {
        String safe = (value == null || value.isBlank() ? "default" : value.trim().toLowerCase(Locale.ROOT)).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }

    private String id() {
        return idService.nextId();
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(Sql sql);
    }
}
