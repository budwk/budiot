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
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
public class DeviceArchiveDefaultQueryService {
    private static final List<String> RAW_BASE_TABLES = List.of("device_raw_log");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private final Dao dao;
    private final DeviceArchiveProperties properties;

    public DeviceArchiveDefaultQueryService(Dao dao, DeviceArchiveProperties properties) {
        this.dao = dao;
        this.properties = properties;
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listRawLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        return queryLogs(RAW_BASE_TABLES, productKey,
                buildTimeCondition("tenantId=@tenantId AND deviceId=@deviceId AND delFlag=false", "deviceAt", startAt, endAt),
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("deviceId", deviceId);
                    bindRange(sql, startAt, endAt);
                    sql.params().set("limit", Math.max(limit, 1));
                },
                this::mapRaw, startAt, endAt);
    }

    public List<Device_raw_log> listRawLogsByRange(String tenantId, long startAt, long endAt) {
        return queryLogs(RAW_BASE_TABLES, null,
                "tenantId=@tenantId AND createdAt>=@startAt AND createdAt<=@endAt AND delFlag=false",
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("startAt", startAt);
                    sql.params().set("endAt", endAt);
                    sql.params().set("limit", Integer.MAX_VALUE);
                },
                this::mapRaw, startAt, endAt);
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listDataLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        return queryLogs(List.of("device_data_log"), productKey,
                buildTimeCondition("tenantId=@tenantId AND deviceId=@deviceId AND delFlag=false", "deviceAt", startAt, endAt),
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("deviceId", deviceId);
                    bindRange(sql, startAt, endAt);
                    sql.params().set("limit", Math.max(limit, 1));
                },
                this::mapData, startAt, endAt);
    }

    public List<Map<String, Object>> listDataRows(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        return queryRows(List.of("device_data_log"), productKey,
                buildTimeCondition("tenantId=@tenantId AND deviceId=@deviceId AND delFlag=false", "deviceAt", startAt, endAt),
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("deviceId", deviceId);
                    bindRange(sql, startAt, endAt);
                    sql.params().set("limit", Math.max(limit, 1));
                }, startAt, endAt);
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listEventLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        return queryLogs(List.of("device_event_log"), productKey,
                buildTimeCondition("tenantId=@tenantId AND deviceId=@deviceId AND delFlag=false", "deviceAt", startAt, endAt),
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("deviceId", deviceId);
                    bindRange(sql, startAt, endAt);
                    sql.params().set("limit", Math.max(limit, 1));
                },
                this::mapEvent, startAt, endAt);
    }

    public List<Device_command_log> listCommandLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listCommandLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_command_log> listCommandLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        return queryLogs(List.of("device_command_log"), productKey,
                buildTimeCondition("tenantId=@tenantId AND deviceId=@deviceId AND delFlag=false", "createdAt", startAt, endAt),
                sql -> {
                    sql.params().set("tenantId", tenantId);
                    sql.params().set("deviceId", deviceId);
                    bindRange(sql, startAt, endAt);
                    sql.params().set("limit", Math.max(limit, 1));
                },
                this::mapCommand, startAt, endAt);
    }

    public long countRawLogs(String tenantId, long startAt, long endAt) {
        return count(RAW_BASE_TABLES, "tenantId=@tenantId AND createdAt>=@startAt AND createdAt<=@endAt AND delFlag=false", sql -> {
            sql.params().set("tenantId", tenantId);
            sql.params().set("startAt", startAt);
            sql.params().set("endAt", endAt);
        }, startAt, endAt);
    }

    public long countHistoricalActiveDevices(String tenantId, long startAt, long endAt) {
        List<String> tables = resolveTables(RAW_BASE_TABLES, null, startAt, endAt);
        if (tables.isEmpty()) {
            return 0L;
        }
        List<String> selects = tables.stream()
                .map(table -> "SELECT deviceId FROM " + table + " WHERE tenantId=@tenantId AND createdAt>=@startAt AND createdAt<=@endAt AND delFlag=false")
                .toList();
        String source = selects.size() == 1 ? selects.getFirst() : String.join(" UNION ALL ", selects);
        List<Long> totals = new ArrayList<>();
        Sql sql = Sqls.create("SELECT COUNT(DISTINCT deviceId) AS total FROM (" + source + ") t");
        sql.params().set("tenantId", tenantId);
        sql.params().set("startAt", startAt);
        sql.params().set("endAt", endAt);
        sql.setCallback((conn, rs, sql1) -> {
            if (rs.next()) {
                totals.add(rs.getLong("total"));
            }
            return null;
        });
        dao.execute(sql);
        return totals.isEmpty() ? 0L : totals.getFirst();
    }

    public long countEventLogs(String tenantId, long startAt, long endAt, List<DeviceEventSourceType> sourceTypes) {
        StringBuilder condition = new StringBuilder("tenantId=@tenantId AND createdAt>=@startAt AND createdAt<=@endAt AND delFlag=false");
        if (sourceTypes != null && !sourceTypes.isEmpty()) {
            String joined = sourceTypes.stream().map(DeviceEventSourceType::getValue).map(v -> "'" + v + "'").reduce((a, b) -> a + "," + b).orElse("");
            condition.append(" AND sourceType IN (").append(joined).append(")");
        }
        return count(List.of("device_event_log"), condition.toString(), sql -> {
            sql.params().set("tenantId", tenantId);
            sql.params().set("startAt", startAt);
            sql.params().set("endAt", endAt);
        }, startAt, endAt);
    }

    private long count(List<String> baseTables, String condition, Consumer<Sql> binder, Long startAt, Long endAt) {
        List<String> tables = resolveTables(baseTables, null, startAt, endAt);
        if (tables.isEmpty()) {
            return 0L;
        }
        List<String> selects = tables.stream()
                .map(table -> "SELECT COUNT(1) AS cnt FROM " + table + " WHERE " + condition)
                .toList();
        String source = selects.size() == 1 ? selects.getFirst() : String.join(" UNION ALL ", selects);
        List<Long> totals = new ArrayList<>();
        Sql sql = Sqls.create("SELECT COALESCE(SUM(cnt),0) AS total FROM (" + source + ") t");
        binder.accept(sql);
        sql.setCallback((conn, rs, sql1) -> {
            if (rs.next()) {
                totals.add(rs.getLong("total"));
            }
            return null;
        });
        dao.execute(sql);
        return totals.isEmpty() ? 0L : totals.getFirst();
    }

    private <T> List<T> queryLogs(List<String> baseTables, String productKey, String condition, Consumer<Sql> binder, RowMapper<T> mapper, Long startAt, Long endAt) {
        List<String> tables = resolveTables(baseTables, productKey, startAt, endAt);
        if (tables.isEmpty()) {
            return List.of();
        }
        List<String> selects = tables.stream()
                .map(table -> "SELECT * FROM " + table + " WHERE " + condition)
                .toList();
        String source = selects.size() == 1 ? selects.getFirst() : String.join(" UNION ALL ", selects);
        List<T> list = new ArrayList<>();
        Sql sql = Sqls.create("SELECT * FROM (" + source + ") t ORDER BY createdAt DESC LIMIT @limit");
        binder.accept(sql);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                try {
                    list.add(mapper.map(rs));
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            return null;
        });
        dao.execute(sql);
        return list;
    }

    private List<Map<String, Object>> queryRows(List<String> baseTables, String productKey, String condition, Consumer<Sql> binder, Long startAt, Long endAt) {
        List<String> tables = resolveTables(baseTables, productKey, startAt, endAt);
        if (tables.isEmpty()) {
            return List.of();
        }
        List<String> selects = tables.stream()
                .map(table -> "SELECT * FROM " + table + " WHERE " + condition)
                .toList();
        String source = selects.size() == 1 ? selects.getFirst() : String.join(" UNION ALL ", selects);
        List<Map<String, Object>> list = new ArrayList<>();
        Sql sql = Sqls.create("SELECT * FROM (" + source + ") t ORDER BY createdAt DESC LIMIT @limit");
        binder.accept(sql);
        sql.setCallback((conn, rs, sql1) -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                }
                list.add(row);
            }
            return null;
        });
        dao.execute(sql);
        return list;
    }

    private List<String> resolveTables(List<String> baseTables, String productKey, Long startAt, Long endAt) {
        Set<String> tables = new LinkedHashSet<>();
        for (String baseTable : baseTables) {
            if (Strings.isNotBlank(productKey)) {
                if (startAt != null && endAt != null) {
                    for (String tableName : resolveMonthlyTables(baseTable, sanitize(productKey), startAt, endAt)) {
                        if (tableExists(tableName)) {
                            tables.add(tableName);
                        }
                    }
                } else {
                    tables.addAll(queryTableNames(baseTable + "_" + sanitize(productKey) + "_%"));
                }
                continue;
            }
            if (startAt != null && endAt != null) {
                for (String suffix : resolveMonthSuffixes(startAt, endAt)) {
                    tables.addAll(queryTableNames(baseTable + "_%_" + suffix));
                }
            } else {
                tables.addAll(queryTableNames(baseTable + "_%"));
            }
        }
        return new ArrayList<>(tables);
    }

    private List<String> resolveMonthlyTables(String baseTable, String productKey, long startAt, long endAt) {
        List<String> tables = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(Instant.ofEpochMilli(Math.min(startAt, endAt)).atZone(ZoneId.systemDefault()));
        YearMonth endMonth = YearMonth.from(Instant.ofEpochMilli(Math.max(startAt, endAt)).atZone(ZoneId.systemDefault()));
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            tables.add(baseTable + "_" + productKey + "_" + MONTH_FORMATTER.format(current));
            current = current.plusMonths(1);
        }
        return tables;
    }

    private List<String> resolveMonthSuffixes(long startAt, long endAt) {
        List<String> suffixes = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(Instant.ofEpochMilli(Math.min(startAt, endAt)).atZone(ZoneId.systemDefault()));
        YearMonth endMonth = YearMonth.from(Instant.ofEpochMilli(Math.max(startAt, endAt)).atZone(ZoneId.systemDefault()));
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            suffixes.add(MONTH_FORMATTER.format(current));
            current = current.plusMonths(1);
        }
        return suffixes;
    }

    private void bindRange(Sql sql, Long startAt, Long endAt) {
        if (startAt != null) {
            sql.params().set("startAt", startAt);
        }
        if (endAt != null) {
            sql.params().set("endAt", endAt);
        }
    }

    private String buildTimeCondition(String baseCondition, String timeColumn, Long startAt, Long endAt) {
        StringBuilder builder = new StringBuilder(baseCondition);
        if (startAt != null) {
            builder.append(" AND ").append(timeColumn).append(">=@startAt");
        }
        if (endAt != null) {
            builder.append(" AND ").append(timeColumn).append("<=@endAt");
        }
        return builder.toString();
    }

    private boolean tableExists(String tableName) {
        List<String> list = new ArrayList<>();
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return null;
        });
        dao.execute(sql);
        return !list.isEmpty();
    }

    private List<String> queryTableNames(String pattern) {
        List<String> tableNames = new ArrayList<>();
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name LIKE @pattern");
        sql.params().set("pattern", pattern);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
            return null;
        });
        dao.execute(sql);
        return tableNames;
    }

    private Device_raw_log mapRaw(ResultSet rs) throws Exception {
        Device_raw_log log = new Device_raw_log();
        fillBase(log, rs);
        log.setId(rs.getString("id"));
        log.setTenantId(rs.getString("tenantId"));
        log.setDeviceId(rs.getString("deviceId"));
        log.setDeviceCode(rs.getString("deviceCode"));
        log.setProductId(rs.getString("productId"));
        log.setMessageId(rs.getString("messageId"));
        log.setDirection(parseDirection(rs.getString("direction")));
        log.setMessageType(parseMessageType(rs.getString("messageType")));
        log.setProtocol(rs.getString("protocol"));
        log.setTopic(rs.getString("topic"));
        log.setGatewayNodeId(rs.getString("gatewayNodeId"));
        log.setPayload(rs.getString("payload"));
        log.setParsedJson(rs.getString("parsedJson"));
        log.setSuccess(rs.getBoolean("success"));
        log.setDeviceAt(readLong(rs, "deviceAt"));
        return log;
    }

    private Device_data_log mapData(ResultSet rs) throws Exception {
        Device_data_log log = new Device_data_log();
        fillBase(log, rs);
        log.setId(rs.getString("id"));
        log.setTenantId(rs.getString("tenantId"));
        log.setDeviceId(rs.getString("deviceId"));
        log.setDeviceCode(rs.getString("deviceCode"));
        log.setProductId(rs.getString("productId"));
        log.setMessageId(rs.getString("messageId"));
        log.setIdentifier(rs.getString("identifier"));
        log.setName(rs.getString("name"));
        log.setValueJson(rs.getString("valueJson"));
        log.setUnit(rs.getString("unit"));
        log.setDeviceAt(readLong(rs, "deviceAt"));
        return log;
    }

    private Device_event_log mapEvent(ResultSet rs) throws Exception {
        Device_event_log log = new Device_event_log();
        fillBase(log, rs);
        log.setId(rs.getString("id"));
        log.setTenantId(rs.getString("tenantId"));
        log.setDeviceId(rs.getString("deviceId"));
        log.setDeviceCode(rs.getString("deviceCode"));
        log.setProductId(rs.getString("productId"));
        log.setEventCode(rs.getString("eventCode"));
        log.setEventName(rs.getString("eventName"));
        log.setLevel(parseEventLevel(rs.getString("level")));
        log.setSourceType(parseEventSource(rs.getString("sourceType")));
        log.setContentJson(rs.getString("contentJson"));
        log.setHandled(rs.getBoolean("handled"));
        log.setDeviceAt(readLong(rs, "deviceAt"));
        return log;
    }

    private Device_command_log mapCommand(ResultSet rs) throws Exception {
        Device_command_log log = new Device_command_log();
        fillBase(log, rs);
        log.setId(rs.getString("id"));
        log.setTenantId(rs.getString("tenantId"));
        log.setDeviceId(rs.getString("deviceId"));
        log.setDeviceCode(rs.getString("deviceCode"));
        log.setProductId(rs.getString("productId"));
        log.setCommandCode(rs.getString("commandCode"));
        log.setPayloadJson(rs.getString("payloadJson"));
        log.setReplyRequired(rs.getBoolean("replyRequired"));
        log.setDeadlineAt(readLong(rs, "deadlineAt"));
        log.setMessageId(rs.getString("messageId"));
        log.setMessageTopic(rs.getString("messageTopic"));
        log.setQueuedAt(readLong(rs, "queuedAt"));
        log.setFinishedAt(readLong(rs, "finishedAt"));
        log.setResponseJson(rs.getString("responseJson"));
        log.setErrorMessage(rs.getString("errorMessage"));
        log.setStatus(parseCommandStatus(rs.getString("status")));
        return log;
    }

    private void fillBase(com.budwk.sp.starter.database.entity.BaseEntity entity, ResultSet rs) throws Exception {
        entity.setCreatedBy(rs.getString("createdBy"));
        entity.setCreatedAt(readLong(rs, "createdAt"));
        entity.setUpdatedBy(rs.getString("updatedBy"));
        entity.setUpdatedAt(readLong(rs, "updatedAt"));
        entity.setDelFlag(rs.getBoolean("delFlag"));
    }

    private Long readLong(ResultSet rs, String column) throws Exception {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private DeviceMessageDirection parseDirection(String value) {
        for (DeviceMessageDirection item : DeviceMessageDirection.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sNull(value).trim())) {
                return item;
            }
        }
        return null;
    }

    private DeviceMessageType parseMessageType(String value) {
        for (DeviceMessageType item : DeviceMessageType.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sNull(value).trim())) {
                return item;
            }
        }
        return null;
    }

    private DeviceEventLevel parseEventLevel(String value) {
        for (DeviceEventLevel item : DeviceEventLevel.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sNull(value).trim())) {
                return item;
            }
        }
        return null;
    }

    private DeviceEventSourceType parseEventSource(String value) {
        for (DeviceEventSourceType item : DeviceEventSourceType.values()) {
            if (item.getValue().equalsIgnoreCase(Strings.sNull(value).trim())) {
                return item;
            }
        }
        return null;
    }

    private DeviceCommandStatus parseCommandStatus(String value) {
        for (DeviceCommandStatus item : DeviceCommandStatus.values()) {
            if (item.name().equalsIgnoreCase(Strings.sNull(value).trim())) {
                return item;
            }
        }
        return null;
    }

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws Exception;
    }
}
