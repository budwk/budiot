package com.budwk.sp.device.database.service;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class TdengineArchiveService {
    private final DeviceDatabaseProperties properties;
    private final ObjectProvider<Dao> tdengineDaoProvider;
    private final Queue<Row> rawRows = new ConcurrentLinkedQueue<>();
    private final Queue<Row> dataRows = new ConcurrentLinkedQueue<>();
    private final Queue<Row> eventRows = new ConcurrentLinkedQueue<>();
    private final Queue<Row> commandRows = new ConcurrentLinkedQueue<>();
    private final Set<String> stableCache = ConcurrentHashMap.newKeySet();

    public TdengineArchiveService(DeviceDatabaseProperties properties,
                                 @Qualifier("tdengineDao") ObjectProvider<Dao> tdengineDaoProvider) {
        this.properties = properties;
        this.tdengineDaoProvider = tdengineDaoProvider;
    }

    public void enqueueRaw(String productKey, long ts, String tenantId, String productId, String deviceId, String deviceCode,
                           String messageId, String direction, String messageType, String protocol, String topic, String gatewayNodeId,
                           String payload, String parsedJson, boolean success) {
        if (!enabled(productKey)) return;
        rawRows.offer(new Row(productKey, ts, List.of(tenantId, productId, deviceId, deviceCode, messageId, direction, messageType, protocol, topic, gatewayNodeId, payload, parsedJson, String.valueOf(success))));
        flushIfNecessary();
    }

    public void enqueueData(String productKey, long ts, String tenantId, String productId, String deviceId, String deviceCode,
                            String messageId, String identifier, String name, String valueJson, String unit) {
        if (!enabled(productKey)) return;
        dataRows.offer(new Row(productKey, ts, List.of(tenantId, productId, deviceId, deviceCode, messageId, identifier, name, valueJson, unit)));
        flushIfNecessary();
    }

    public void enqueueEvent(String productKey, long ts, String tenantId, String productId, String deviceId, String deviceCode,
                             String eventCode, String eventName, String level, String sourceType, String contentJson) {
        if (!enabled(productKey)) return;
        eventRows.offer(new Row(productKey, ts, List.of(tenantId, productId, deviceId, deviceCode, eventCode, eventName, level, sourceType, contentJson)));
        flushIfNecessary();
    }

    public void enqueueCommand(String productKey, long ts, String tenantId, String productId, String deviceId, String deviceCode,
                               String commandCode, String payloadJson, boolean replyRequired, Long deadlineAt,
                               String messageId, String messageTopic, Long queuedAt, Long finishedAt,
                               String responseJson, String errorMessage, String status) {
        if (!enabled(productKey)) return;
        commandRows.offer(new Row(productKey, ts, List.of(tenantId, productId, deviceId, deviceCode, commandCode, payloadJson,
                String.valueOf(replyRequired), String.valueOf(deadlineAt), messageId, messageTopic, String.valueOf(queuedAt),
                String.valueOf(finishedAt), responseJson, errorMessage, status)));
        flushIfNecessary();
    }

    @Scheduled(fixedDelayString = "${wk.device.database-ext.tdengine-flush-seconds:2}000")
    public void flushScheduled() {
        flush();
    }

    public synchronized void flush() {
        Dao dao = tdengineDaoProvider.getIfAvailable();
        if (!properties.isTdengineEnabled() || dao == null || Strings.isBlank(properties.getTdengineUrl())) {
            return;
        }
        flushRaw(dao);
        flushData(dao);
        flushEvents(dao);
        flushCommands(dao);
    }

    private void flushRaw(Dao dao) {
        List<Row> batch = poll(rawRows);
        if (batch.isEmpty()) return;
        try {
            for (Row row : batch) {
                String stable = stableName("raw", row.productKey());
                ensureStable(dao, stable, "(ts TIMESTAMP, message_id BINARY(80), direction BINARY(20), message_type BINARY(20), protocol BINARY(20), topic BINARY(120), gateway_node_id BINARY(64), payload NCHAR(4096), parsed_json NCHAR(8192), success BOOL)");
                String sqlText = "INSERT INTO " + tableName("raw", row.productKey(), row.values().get(2)) + " USING " + stable +
                        " TAGS (@tenantId,@productId,@deviceId,@deviceCode) VALUES (@ts,@messageId,@direction,@messageType,@protocol,@topic,@gatewayNodeId,@payload,@parsedJson,@success)";
                Sql sql = Sqls.create(sqlText);
                bindCommonTags(sql, row.values());
                sql.params().set("ts", row.ts());
                sql.params().set("messageId", valueAt(row.values(), 4));
                sql.params().set("direction", valueAt(row.values(), 5));
                sql.params().set("messageType", valueAt(row.values(), 6));
                sql.params().set("protocol", valueAt(row.values(), 7));
                sql.params().set("topic", valueAt(row.values(), 8));
                sql.params().set("gatewayNodeId", valueAt(row.values(), 9));
                sql.params().set("payload", valueAt(row.values(), 10));
                sql.params().set("parsedJson", valueAt(row.values(), 11));
                sql.params().set("success", Boolean.parseBoolean(valueAt(row.values(), 12)));
                dao.execute(sql);
            }
        } catch (Exception e) {
            batch.forEach(rawRows::offer);
            log.error("flush tdengine raw failed", e);
        }
    }

    private void flushData(Dao dao) {
        List<Row> batch = poll(dataRows);
        if (batch.isEmpty()) return;
        try {
            for (Row row : batch) {
                String stable = stableName("data", row.productKey());
                ensureStable(dao, stable, "(ts TIMESTAMP, message_id BINARY(80), identifier BINARY(80), name BINARY(120), value_json NCHAR(2048), unit BINARY(32))");
                String sqlText = "INSERT INTO " + tableName("data", row.productKey(), row.values().get(2)) + " USING " + stable +
                        " TAGS (@tenantId,@productId,@deviceId,@deviceCode) VALUES (@ts,@messageId,@identifier,@name,@valueJson,@unit)";
                Sql sql = Sqls.create(sqlText);
                bindCommonTags(sql, row.values());
                sql.params().set("ts", row.ts());
                sql.params().set("messageId", valueAt(row.values(), 4));
                sql.params().set("identifier", valueAt(row.values(), 5));
                sql.params().set("name", valueAt(row.values(), 6));
                sql.params().set("valueJson", valueAt(row.values(), 7));
                sql.params().set("unit", valueAt(row.values(), 8));
                dao.execute(sql);
            }
        } catch (Exception e) {
            batch.forEach(dataRows::offer);
            log.error("flush tdengine data failed", e);
        }
    }

    private void flushEvents(Dao dao) {
        List<Row> batch = poll(eventRows);
        if (batch.isEmpty()) return;
        try {
            for (Row row : batch) {
                String stable = stableName("event", row.productKey());
                ensureStable(dao, stable, "(ts TIMESTAMP, event_code BINARY(80), event_name BINARY(120), level BINARY(20), source_type BINARY(20), content_json NCHAR(2048))");
                String sqlText = "INSERT INTO " + tableName("event", row.productKey(), row.values().get(2)) + " USING " + stable +
                        " TAGS (@tenantId,@productId,@deviceId,@deviceCode) VALUES (@ts,@eventCode,@eventName,@level,@sourceType,@contentJson)";
                Sql sql = Sqls.create(sqlText);
                bindCommonTags(sql, row.values());
                sql.params().set("ts", row.ts());
                sql.params().set("eventCode", valueAt(row.values(), 4));
                sql.params().set("eventName", valueAt(row.values(), 5));
                sql.params().set("level", valueAt(row.values(), 6));
                sql.params().set("sourceType", valueAt(row.values(), 7));
                sql.params().set("contentJson", valueAt(row.values(), 8));
                dao.execute(sql);
            }
        } catch (Exception e) {
            batch.forEach(eventRows::offer);
            log.error("flush tdengine event failed", e);
        }
    }

    private void flushCommands(Dao dao) {
        List<Row> batch = poll(commandRows);
        if (batch.isEmpty()) return;
        try {
            for (Row row : batch) {
                String stable = stableName("cmd", row.productKey());
                ensureStable(dao, stable, "(ts TIMESTAMP, command_code BINARY(80), payload_json NCHAR(4096), reply_required BOOL, deadline_at BIGINT, message_id BINARY(80), message_topic BINARY(160), queued_at BIGINT, finished_at BIGINT, response_json NCHAR(4096), error_message BINARY(255), status BINARY(20))");
                String sqlText = "INSERT INTO " + tableName("cmd", row.productKey(), row.values().get(2)) + " USING " + stable +
                        " TAGS (@tenantId,@productId,@deviceId,@deviceCode) VALUES (@ts,@commandCode,@payloadJson,@replyRequired,@deadlineAt,@messageId,@messageTopic,@queuedAt,@finishedAt,@responseJson,@errorMessage,@status)";
                Sql sql = Sqls.create(sqlText);
                bindCommonTags(sql, row.values());
                sql.params().set("ts", row.ts());
                sql.params().set("commandCode", valueAt(row.values(), 4));
                sql.params().set("payloadJson", valueAt(row.values(), 5));
                sql.params().set("replyRequired", Boolean.parseBoolean(valueAt(row.values(), 6)));
                sql.params().set("deadlineAt", parseLong(row.values(), 7));
                sql.params().set("messageId", valueAt(row.values(), 8));
                sql.params().set("messageTopic", valueAt(row.values(), 9));
                sql.params().set("queuedAt", parseLong(row.values(), 10));
                sql.params().set("finishedAt", parseLong(row.values(), 11));
                sql.params().set("responseJson", valueAt(row.values(), 12));
                sql.params().set("errorMessage", valueAt(row.values(), 13));
                sql.params().set("status", valueAt(row.values(), 14));
                dao.execute(sql);
            }
        } catch (Exception e) {
            batch.forEach(commandRows::offer);
            log.error("flush tdengine command failed", e);
        }
    }

    private void bindCommonTags(Sql sql, List<String> values) {
        sql.params().set("tenantId", valueAt(values, 0));
        sql.params().set("productId", valueAt(values, 1));
        sql.params().set("deviceId", valueAt(values, 2));
        sql.params().set("deviceCode", valueAt(values, 3));
    }

    private String valueAt(List<String> values, int index) {
        return Strings.sNull(values.get(index)).trim();
    }

    private Long parseLong(List<String> values, int index) {
        String value = valueAt(values, index);
        return Strings.isBlank(value) || "null".equalsIgnoreCase(value) ? null : Long.parseLong(value);
    }

    private void ensureStable(Dao dao, String stable, String fields) {
        if (!stableCache.add(stable)) {
            return;
        }
        Sql sql = Sqls.create("CREATE STABLE IF NOT EXISTS " + stable + " " + fields + " TAGS (tenant_id BINARY(32), product_id BINARY(32), device_id BINARY(32), device_code BINARY(64))");
        dao.execute(sql);
    }

    private void flushIfNecessary() {
        if (rawRows.size() + dataRows.size() + eventRows.size() + commandRows.size() >= Math.max(properties.getTdengineBatchSize(), 1)) {
            flush();
        }
    }

    private boolean enabled(String productKey) {
        return properties.isTdengineEnabled() && Strings.isNotBlank(properties.getTdengineUrl()) && Strings.isNotBlank(productKey);
    }

    private List<Row> poll(Queue<Row> queue) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < Math.max(properties.getTdengineBatchSize(), 1); i++) {
            Row row = queue.poll();
            if (row == null) break;
            rows.add(row);
        }
        return rows;
    }

    private String stableName(String prefix, String productKey) {
        return "st_device_" + prefix + "_" + sanitize(productKey);
    }

    private String tableName(String prefix, String productKey, String deviceId) {
        return "t_device_" + prefix + "_" + sanitize(productKey) + "_" + sanitize(deviceId);
    }

    private String sanitize(String value) {
        String safe = Strings.sBlank(value, "default").toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }

    @PreDestroy
    public void destroy() {
        log.info("TDengine 归档服务优雅停机，强制刷写队列数据...");
        flush();
        log.info("TDengine 归档服务已停止，剩余队列: raw={}, data={}, events={}, commands={}",
                rawRows.size(), dataRows.size(), eventRows.size(), commandRows.size());
    }

    private record Row(String productKey, long ts, List<String> values) {
    }
}
