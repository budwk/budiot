package com.budwk.sp.device.services.support;

import com.budwk.sp.device.config.DeviceArchiveProperties;
import com.budwk.sp.device.entity.Device_data_log;
import com.budwk.sp.device.entity.Device_event_log;
import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.enums.DeviceEventLevel;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.device.enums.DeviceMessageDirection;
import com.budwk.sp.device.enums.DeviceMessageType;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class DeviceArchiveTdengineQueryService {
    private final DeviceArchiveProperties properties;
    private final ObjectProvider<Dao> tdengineDaoProvider;

    public DeviceArchiveTdengineQueryService(DeviceArchiveProperties properties,
                                            @Qualifier("tdengineDao") ObjectProvider<Dao> tdengineDaoProvider) {
        this.properties = properties;
        this.tdengineDaoProvider = tdengineDaoProvider;
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listRawLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_raw_log> listRawLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_raw_log> logs = new ArrayList<>();
        for (String stable : resolveStables(List.of("raw", "msg"), productKey)) {
            String sqlText = "SELECT ts, tenant_id, product_id, device_id, device_code, direction, message_type, protocol, topic, gateway_node_id, payload, success FROM " + stable +
                    " WHERE tenant_id=@tenantId AND device_id=@deviceId" + buildTimeCondition("ts", startAt, endAt) + " ORDER BY ts DESC LIMIT " + Math.max(limit, 1);
            logs.addAll(query(sqlText, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("deviceId", deviceId);
                bindRange(sql, startAt, endAt);
            }, this::mapRaw));
        }
        return logs.stream().sorted(Comparator.comparing(Device_raw_log::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(Math.max(limit, 1)).toList();
    }

    public List<Device_raw_log> listRawLogsByRange(String tenantId, long startAt, long endAt) {
        List<Device_raw_log> logs = new ArrayList<>();
        for (String stable : resolveStables(List.of("raw", "msg"), null)) {
            String sqlText = "SELECT ts, tenant_id, product_id, device_id, device_code, direction, message_type, protocol, topic, gateway_node_id, payload, success FROM " + stable +
                    " WHERE tenant_id=@tenantId AND ts>=@startAt AND ts<=@endAt";
            logs.addAll(query(sqlText, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("startAt", startAt);
                sql.params().set("endAt", endAt);
            }, this::mapRaw));
        }
        return logs.stream().sorted(Comparator.comparing(Device_raw_log::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listDataLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_data_log> listDataLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_data_log> logs = new ArrayList<>();
        for (String stable : resolveStables(List.of("data"), productKey)) {
            String sqlText = "SELECT ts, tenant_id, product_id, device_id, device_code, identifier, name, value_json, unit FROM " + stable +
                    " WHERE tenant_id=@tenantId AND device_id=@deviceId" + buildTimeCondition("ts", startAt, endAt) + " ORDER BY ts DESC LIMIT " + Math.max(limit, 1);
            logs.addAll(query(sqlText, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("deviceId", deviceId);
                bindRange(sql, startAt, endAt);
            }, this::mapData));
        }
        return logs.stream().sorted(Comparator.comparing(Device_data_log::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(Math.max(limit, 1)).toList();
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, int limit) {
        return listEventLogs(tenantId, deviceId, productKey, null, null, limit);
    }

    public List<Device_event_log> listEventLogs(String tenantId, String deviceId, String productKey, Long startAt, Long endAt, int limit) {
        List<Device_event_log> logs = new ArrayList<>();
        for (String stable : resolveStables(List.of("event", "evt"), productKey)) {
            String sqlText = "SELECT ts, tenant_id, product_id, device_id, device_code, event_code, event_name, level, source_type, content_json FROM " + stable +
                    " WHERE tenant_id=@tenantId AND device_id=@deviceId" + buildTimeCondition("ts", startAt, endAt) + " ORDER BY ts DESC LIMIT " + Math.max(limit, 1);
            logs.addAll(query(sqlText, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("deviceId", deviceId);
                bindRange(sql, startAt, endAt);
            }, this::mapEvent));
        }
        return logs.stream().sorted(Comparator.comparing(Device_event_log::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(Math.max(limit, 1)).toList();
    }

    public long countRawLogs(String tenantId, long startAt, long endAt) {
        long total = 0L;
        for (String stable : resolveStables(List.of("raw", "msg"), null)) {
            total += count(stable, "tenant_id=@tenantId AND ts>=@startAt AND ts<=@endAt", sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("startAt", startAt);
                sql.params().set("endAt", endAt);
            });
        }
        return total;
    }

    public long countHistoricalActiveDevices(String tenantId, long startAt, long endAt) {
        Set<String> deviceIds = new LinkedHashSet<>();
        for (String stable : resolveStables(List.of("raw", "msg"), null)) {
            String sqlText = "SELECT DISTINCT device_id FROM " + stable + " WHERE tenant_id=@tenantId AND ts>=@startAt AND ts<=@endAt";
            List<String> ids = query(sqlText, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("startAt", startAt);
                sql.params().set("endAt", endAt);
            }, rs -> rs.getString("device_id"));
            deviceIds.addAll(ids);
        }
        return deviceIds.size();
    }

    public long countEventLogs(String tenantId, long startAt, long endAt, List<DeviceEventSourceType> sourceTypes) {
        String condition = buildEventCondition(sourceTypes);
        long total = 0L;
        for (String stable : resolveStables(List.of("event", "evt"), null)) {
            total += count(stable, condition, sql -> {
                sql.params().set("tenantId", tenantId);
                sql.params().set("startAt", startAt);
                sql.params().set("endAt", endAt);
            });
        }
        return total;
    }

    private String buildEventCondition(List<DeviceEventSourceType> sourceTypes) {
        StringBuilder builder = new StringBuilder("tenant_id=@tenantId AND ts>=@startAt AND ts<=@endAt");
        if (sourceTypes != null && !sourceTypes.isEmpty()) {
            String joined = sourceTypes.stream().map(DeviceEventSourceType::getValue).map(value -> "'" + value + "'").reduce((a, b) -> a + "," + b).orElse("");
            builder.append(" AND source_type IN (").append(joined).append(")");
        }
        return builder.toString();
    }

    private long count(String stable, String condition, ParamBinder binder) {
        List<Long> totals = query("SELECT COUNT(1) AS total FROM " + stable + " WHERE " + condition, binder, rs -> rs.getLong("total"));
        return totals.isEmpty() ? 0L : totals.getFirst();
    }

    private void bindRange(Sql sql, Long startAt, Long endAt) {
        if (startAt != null) {
            sql.params().set("startAt", startAt);
        }
        if (endAt != null) {
            sql.params().set("endAt", endAt);
        }
    }

    private String buildTimeCondition(String column, Long startAt, Long endAt) {
        StringBuilder builder = new StringBuilder();
        if (startAt != null) {
            builder.append(" AND ").append(column).append(">=@startAt");
        }
        if (endAt != null) {
            builder.append(" AND ").append(column).append("<=@endAt");
        }
        return builder.toString();
    }

    private <T> List<T> query(String sqlText, ParamBinder binder, RowMapper<T> mapper) {
        Dao dao = tdengineDaoProvider.getIfAvailable();
        if (!properties.isTdengineEnabled() || dao == null || Strings.isBlank(properties.getTdengineUrl())) {
            return List.of();
        }
        List<T> list = new ArrayList<>();
        try {
            Sql sql = Sqls.create(sqlText);
            binder.bind(sql);
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
        } catch (Exception e) {
            log.error("query tdengine archive failed: {}", sqlText, e);
        }
        return list;
    }

    private List<String> resolveStables(List<String> categories, String productKey) {
        Dao dao = tdengineDaoProvider.getIfAvailable();
        if (!properties.isTdengineEnabled() || dao == null || Strings.isBlank(properties.getTdengineUrl())) {
            return List.of();
        }
        Set<String> stables = new LinkedHashSet<>();
        for (String category : categories) {
            String pattern = Strings.isBlank(productKey)
                    ? "st_device_" + category + "_%"
                    : "st_device_" + category + "_" + sanitize(productKey);
            Sql sql = Sqls.create("SHOW STABLES LIKE '" + pattern + "'");
            sql.setCallback((conn, rs, sql1) -> {
                while (rs.next()) {
                    stables.add(rs.getString(1));
                }
                return null;
            });
            dao.execute(sql);
        }
        return new ArrayList<>(stables);
    }

    private Device_raw_log mapRaw(ResultSet rs) throws Exception {
        Device_raw_log log = new Device_raw_log();
        long ts = rs.getTimestamp("ts").getTime();
        fillBase(log, ts);
        log.setId(readId(rs, ts));
        log.setTenantId(rs.getString("tenant_id"));
        log.setDeviceId(rs.getString("device_id"));
        log.setDeviceCode(rs.getString("device_code"));
        log.setProductId(rs.getString("product_id"));
        log.setMessageId(rs.getString("message_id"));
        log.setDirection(parseDirection(rs.getString("direction")));
        log.setMessageType(parseMessageType(rs.getString("message_type")));
        log.setProtocol(rs.getString("protocol"));
        log.setTopic(rs.getString("topic"));
        log.setGatewayNodeId(rs.getString("gateway_node_id"));
        log.setPayload(rs.getString("payload"));
        log.setParsedJson(rs.getString("parsed_json"));
        log.setSuccess(rs.getBoolean("success"));
        log.setDeviceAt(ts);
        return log;
    }

    private Device_data_log mapData(ResultSet rs) throws Exception {
        Device_data_log log = new Device_data_log();
        long ts = rs.getTimestamp("ts").getTime();
        fillBase(log, ts);
        log.setId(readId(rs, ts));
        log.setTenantId(rs.getString("tenant_id"));
        log.setDeviceId(rs.getString("device_id"));
        log.setDeviceCode(rs.getString("device_code"));
        log.setProductId(rs.getString("product_id"));
        log.setMessageId(rs.getString("message_id"));
        log.setIdentifier(rs.getString("identifier"));
        log.setName(rs.getString("name"));
        log.setValueJson(rs.getString("value_json"));
        log.setUnit(rs.getString("unit"));
        log.setDeviceAt(ts);
        return log;
    }

    private Device_event_log mapEvent(ResultSet rs) throws Exception {
        Device_event_log log = new Device_event_log();
        long ts = rs.getTimestamp("ts").getTime();
        fillBase(log, ts);
        log.setId(readId(rs, ts));
        log.setTenantId(rs.getString("tenant_id"));
        log.setDeviceId(rs.getString("device_id"));
        log.setDeviceCode(rs.getString("device_code"));
        log.setProductId(rs.getString("product_id"));
        log.setEventCode(rs.getString("event_code"));
        log.setEventName(rs.getString("event_name"));
        log.setLevel(parseEventLevel(rs.getString("level")));
        log.setSourceType(parseEventSource(rs.getString("source_type")));
        log.setContentJson(rs.getString("content_json"));
        log.setHandled(false);
        log.setDeviceAt(ts);
        return log;
    }

    private void fillBase(com.budwk.sp.starter.database.entity.BaseEntity entity, long ts) {
        entity.setCreatedBy("tdengine");
        entity.setCreatedAt(ts);
        entity.setUpdatedBy("tdengine");
        entity.setUpdatedAt(ts);
        entity.setDelFlag(false);
    }

    private String readId(ResultSet rs, long ts) throws Exception {
        String deviceId = Strings.sBlank(rs.getString("device_id"), "unknown");
        return deviceId + "_" + ts;
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

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }

    @FunctionalInterface
    private interface ParamBinder {
        void bind(Sql sql);
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws Exception;
    }
}
