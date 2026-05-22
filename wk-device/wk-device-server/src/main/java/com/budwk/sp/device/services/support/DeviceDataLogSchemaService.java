package com.budwk.sp.device.services.support;

import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.support.DeviceThingPropertySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DeviceDataLogSchemaService {
    private final Dao dao;
    private final ObjectMapper objectMapper;

    public DeviceDataLogSchemaService(Dao dao, ObjectMapper objectMapper) {
        this.dao = dao;
        this.objectMapper = objectMapper;
    }

    public void syncProduct(Device_product product) {
        if (product == null || Strings.isBlank(product.getProductKey())) {
            return;
        }
        List<Map<String, Object>> properties = DeviceThingPropertySupport.parseProperties(product.getThingPropertyJson(), objectMapper);
        ensureColumns("device_data_log", properties);
        for (String tableName : queryTableNames("device_data_log_" + Strings.sBlank(product.getProductKey(), "").trim().toLowerCase() + "_%")) {
            ensureColumns(tableName, properties);
        }
    }

    private void ensureColumns(String tableName, List<Map<String, Object>> properties) {
        if (!tableExists(tableName)) {
            return;
        }
        dropColumnIfExists(tableName, "identifier");
        dropColumnIfExists(tableName, "name");
        dropColumnIfExists(tableName, "valueJson");
        dropColumnIfExists(tableName, "unit");
        addColumnIfMissing(tableName, "messageId", "VARCHAR(80)");
        for (Map<String, Object> property : properties) {
            String identifier = DeviceThingPropertySupport.readIdentifier(property);
            if (Strings.isBlank(identifier)) {
                continue;
            }
            String columnName = DeviceThingPropertySupport.toColumnName(identifier);
            migrateLegacyColumn(tableName, DeviceThingPropertySupport.toLegacyColumnName(identifier), columnName);
            addColumnIfMissing(tableName, columnName, "TEXT");
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Sql sql = Sqls.create("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + definition);
        dao.execute(sql);
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        Sql sql = Sqls.create("ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + columnName);
        dao.execute(sql);
    }

    private void migrateLegacyColumn(String tableName, String legacyColumnName, String newColumnName) {
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
}
