package com.budwk.sp.device.support;

import jakarta.annotation.PostConstruct;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Component
public class DeviceCommandSchemaService {
    private final Dao dao;

    public DeviceCommandSchemaService(Dao dao) {
        this.dao = dao;
    }

    @PostConstruct
    public void init() {
        ensurePendingColumns("device_command");
        ensureHistoryColumns("device_command_log");
    }

    public void ensurePendingColumns(String tableName) {
        ensureColumns(tableName, List.of(
                new ColumnDef("messageId", "VARCHAR(64)"),
                new ColumnDef("messageTopic", "VARCHAR(160)"),
                new ColumnDef("queuedAt", "BIGINT"),
                new ColumnDef("sentAt", "BIGINT"),
                new ColumnDef("finishedAt", "BIGINT"),
                new ColumnDef("responseJson", "TEXT"),
                new ColumnDef("errorMessage", "VARCHAR(255)"),
                new ColumnDef("status", "VARCHAR(20)")
        ));
    }

    public void ensureHistoryColumns(String tableName) {
        ensureColumns(tableName, List.of(
                new ColumnDef("messageId", "VARCHAR(64)"),
                new ColumnDef("messageTopic", "VARCHAR(160)"),
                new ColumnDef("queuedAt", "BIGINT"),
                new ColumnDef("sentAt", "BIGINT"),
                new ColumnDef("finishedAt", "BIGINT"),
                new ColumnDef("responseJson", "TEXT"),
                new ColumnDef("errorMessage", "VARCHAR(255)"),
                new ColumnDef("status", "VARCHAR(20)")
        ));
    }

    public boolean tableExists(String tableName) {
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> rs.next() ? rs.getString(1) : null);
        dao.execute(sql);
        return Strings.isNotBlank(sql.getString());
    }

    public String normalizePartitionKey(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }

    public String resolveHistoryTable(String productKey, Long ts) {
        long effectiveTs = ts == null || ts <= 0L ? System.currentTimeMillis() : ts;
        LocalDateTime dateTime = Instant.ofEpochMilli(effectiveTs).atZone(ZoneId.systemDefault()).toLocalDateTime();
        return "device_command_log_" + normalizePartitionKey(productKey) + "_" + String.format("%d%02d", dateTime.getYear(), dateTime.getMonthValue());
    }

    public boolean ensureHistoryTable(String tableName) {
        if (Strings.isBlank(tableName)) {
            return false;
        }
        if (!tableExists(tableName)) {
            if (!tableExists("device_command_log")) {
                return false;
            }
            dao.execute(Sqls.create("CREATE TABLE IF NOT EXISTS " + tableName + " (LIKE device_command_log)"));
        }
        ensureHistoryColumns(tableName);
        return tableExists(tableName);
    }

    private void ensureColumns(String tableName, List<ColumnDef> columns) {
        if (!tableExists(tableName)) {
            return;
        }
        for (ColumnDef column : columns) {
            Sql sql = Sqls.create("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + column.name + " " + column.definition);
            dao.execute(sql);
        }
    }

    private static class ColumnDef {
        private final String name;
        private final String definition;

        private ColumnDef(String name, String definition) {
            this.name = name;
            this.definition = definition;
        }
    }
}
