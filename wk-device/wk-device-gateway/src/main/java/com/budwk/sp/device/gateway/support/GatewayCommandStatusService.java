package com.budwk.sp.device.gateway.support;

import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.support.DeviceCommandSchemaService;
import com.budwk.sp.device.gateway.config.GatewayArchiveProperties;
import com.mongodb.client.result.UpdateResult;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class GatewayCommandStatusService {
    private final Dao dao;
    private final GatewayArchiveProperties archiveProperties;
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final DeviceCommandSchemaService schemaService;

    public GatewayCommandStatusService(Dao dao,
                                       GatewayArchiveProperties archiveProperties,
                                       ObjectProvider<MongoTemplate> mongoTemplateProvider,
                                       DeviceCommandSchemaService schemaService) {
        this.dao = dao;
        this.archiveProperties = archiveProperties;
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.schemaService = schemaService;
    }

    public void updateStatus(DeviceCommandStatusUpdateDTO dto) {
        if (dto == null || (Strings.isBlank(dto.getCommandId()) && Strings.isBlank(dto.getMessageId()))) {
            return;
        }
        schemaService.ensurePendingColumns("device_command");
        Device_command command = resolveCommand(dto);
        if (command == null) {
            updateHistoryOnly(dto);
            return;
        }
        DeviceCommandStatus nextStatus = resolveNextStatus(command, dto);
        Chain chain = Chain.make("status", nextStatus)
                .add("sentAt", resolveSentAt(command, dto))
                .add("finishedAt", dto.getFinishedAt())
                .add("responseJson", Strings.sBlank(dto.getResponseJson(), ""))
                .add("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""))
                .add("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"))
                .add("updatedAt", System.currentTimeMillis());
        if (shouldResetQueuedState(command, dto)) {
            chain.add("messageId", "");
            chain.add("messageTopic", "");
            chain.add("queuedAt", null);
        }
        dao.update(Device_command.class, chain, Cnd.where("id", "=", command.getId()));
        command.setStatus(nextStatus);
        command.setSentAt(resolveSentAt(command, dto));
        command.setFinishedAt(dto.getFinishedAt());
        command.setResponseJson(Strings.sBlank(dto.getResponseJson(), ""));
        command.setErrorMessage(Strings.sBlank(dto.getErrorMessage(), ""));
        if (shouldResetQueuedState(command, dto)) {
            command.setMessageId("");
            command.setMessageTopic("");
            command.setQueuedAt(null);
        }
        boolean historyUpdated = archiveProperties.resolveCommandStorage() == com.budwk.sp.device.enums.DeviceArchiveStorageType.MONGODB
                ? updateMongoCommandLog(command, dto)
                : updateDefaultCommandLog(command, dto);
        if (!historyUpdated) {
            historyUpdated = createHistory(command, dto, nextStatus);
        }
        if (historyUpdated && shouldDeleteCommand(command, nextStatus)) {
            dao.delete(Device_command.class, command.getId());
        }
    }

    private Device_command resolveCommand(DeviceCommandStatusUpdateDTO dto) {
        Device_command command = null;
        if (Strings.isNotBlank(dto.getCommandId())) {
            command = dao.fetch(Device_command.class, dto.getCommandId());
        }
        if (command == null && Strings.isNotBlank(dto.getMessageId())) {
            command = dao.fetch(Device_command.class, Cnd.where("messageId", "=", dto.getMessageId()).and("delFlag", "=", false));
        }
        return command;
    }

    private boolean updateDefaultCommandLog(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        boolean updated = false;
        String productKey = resolveProductKey(command);
        for (String table : resolveRelationalTables(productKey, command == null ? null : (command.getQueuedAt() == null ? command.getCreatedAt() : command.getQueuedAt()))) {
            if (!tableExists(table)) {
                continue;
            }
            schemaService.ensureHistoryColumns(table);
            Sql sql = Sqls.create("UPDATE " + table + " SET status=@status, sentAt=CASE WHEN @sentAt IS NULL THEN sentAt ELSE @sentAt END, finishedAt=@finishedAt, responseJson=@responseJson, errorMessage=@errorMessage, updatedBy=@updatedBy, updatedAt=@updatedAt WHERE messageId=@messageId");
            sql.params().set("status", dto.getStatus() == null ? null : dto.getStatus().name());
            sql.params().set("sentAt", resolveSentAt(command, dto));
            sql.params().set("finishedAt", dto.getFinishedAt());
            sql.params().set("responseJson", Strings.sBlank(dto.getResponseJson(), ""));
            sql.params().set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""));
            sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"));
            sql.params().set("updatedAt", System.currentTimeMillis());
            sql.params().set("messageId", Strings.sBlank(dto.getMessageId(), command.getMessageId()));
            dao.execute(sql);
            updated = updated || sql.getUpdateCount() > 0;
            System.out.println("updated:::"+updated);
        }
        return updated;
    }

    private boolean updateMongoCommandLog(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            return updateDefaultCommandLog(command, dto);
        }
        String collection = archiveProperties.getMongoCollectionPrefix() + "_command_" + normalizeProductKey(resolveProductKey(command));
        Query query = Query.query(Criteria.where("messageId").is(Strings.sBlank(dto.getMessageId(), command.getMessageId())));
        Update update = new Update()
                .set("status", dto.getStatus() == null ? "" : dto.getStatus().name())
                .set("finishedAt", dto.getFinishedAt())
                .set("responseJson", Strings.sBlank(dto.getResponseJson(), ""))
                .set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""))
                .set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"))
                .set("updatedAt", System.currentTimeMillis());
        Long sentAt = resolveSentAt(command, dto);
        if (sentAt != null) {
            update.set("sentAt", sentAt);
        }
        UpdateResult result = mongoTemplate.updateFirst(query, update, collection);
        return result.getMatchedCount() > 0;
    }

    private void updateHistoryOnly(DeviceCommandStatusUpdateDTO dto) {
        if (dto == null || Strings.isBlank(dto.getMessageId())) {
            return;
        }
        if (archiveProperties.resolveCommandStorage() == com.budwk.sp.device.enums.DeviceArchiveStorageType.MONGODB) {
            if (updateMongoCommandLogByMessageId(dto)) {
                return;
            }
        }
        updateDefaultCommandLogByMessageId(dto);
    }

    private boolean updateDefaultCommandLogByMessageId(DeviceCommandStatusUpdateDTO dto) {
        boolean updated = false;
        for (String table : resolveRelationalTables("", null)) {
            if (!tableExists(table)) {
                continue;
            }
            schemaService.ensureHistoryColumns(table);
            Sql sql = Sqls.create("UPDATE " + table + " SET status=@status, sentAt=CASE WHEN @sentAt IS NULL THEN sentAt ELSE @sentAt END, finishedAt=@finishedAt, responseJson=@responseJson, errorMessage=@errorMessage, updatedBy=@updatedBy, updatedAt=@updatedAt WHERE messageId=@messageId");
            sql.params().set("status", dto.getStatus() == null ? null : dto.getStatus().name());
            sql.params().set("sentAt", dto.getSentAt());
            sql.params().set("finishedAt", dto.getFinishedAt());
            sql.params().set("responseJson", Strings.sBlank(dto.getResponseJson(), ""));
            sql.params().set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""));
            sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"));
            sql.params().set("updatedAt", System.currentTimeMillis());
            sql.params().set("messageId", dto.getMessageId());
            dao.execute(sql);
            updated = updated || sql.getUpdateCount() > 0;
        }
        return updated;
    }

    private boolean updateMongoCommandLogByMessageId(DeviceCommandStatusUpdateDTO dto) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            return false;
        }
        Query query = Query.query(Criteria.where("messageId").is(dto.getMessageId()));
        Update update = new Update()
                .set("status", dto.getStatus() == null ? "" : dto.getStatus().name())
                .set("finishedAt", dto.getFinishedAt())
                .set("responseJson", Strings.sBlank(dto.getResponseJson(), ""))
                .set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""))
                .set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"))
                .set("updatedAt", System.currentTimeMillis());
        if (dto.getSentAt() != null) {
            update.set("sentAt", dto.getSentAt());
        }
        String prefix = archiveProperties.getMongoCollectionPrefix() + "_command_";
        for (String collection : mongoTemplate.getCollectionNames()) {
            if (!collection.startsWith(prefix)) {
                continue;
            }
            UpdateResult result = mongoTemplate.updateFirst(query, update, collection);
            if (result.getMatchedCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean createHistory(Device_command command, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        return archiveProperties.resolveCommandStorage() == com.budwk.sp.device.enums.DeviceArchiveStorageType.MONGODB
                ? createMongoHistory(command, dto, status)
                : createRelationalHistory(command, dto, status);
    }

    private boolean createRelationalHistory(Device_command command, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        if (command == null) {
            return false;
        }
        String productKey = resolveProductKey(command);
        Long createdAt = command.getQueuedAt() == null ? command.getCreatedAt() : command.getQueuedAt();
        String table = schemaService.resolveHistoryTable(productKey, createdAt);
        if (!schemaService.ensureHistoryTable(table)) {
            return false;
        }
        Sql sql = Sqls.create("INSERT INTO " + table + " (id, tenantId, deviceId, deviceCode, productId, commandCode, payloadJson, replyRequired, deadlineAt, messageId, messageTopic, queuedAt, sentAt, finishedAt, responseJson, errorMessage, status, createdBy, createdAt, updatedBy, updatedAt, delFlag) VALUES (@id,@tenantId,@deviceId,@deviceCode,@productId,@commandCode,@payloadJson,@replyRequired,@deadlineAt,@messageId,@messageTopic,@queuedAt,@sentAt,@finishedAt,@responseJson,@errorMessage,@status,@createdBy,@createdAt,@updatedBy,@updatedAt,@delFlag)");
        long now = System.currentTimeMillis();
        sql.params().set("id", Strings.sBlank(command.getId(), ""));
        sql.params().set("tenantId", command.getTenantId());
        sql.params().set("deviceId", command.getDeviceId());
        sql.params().set("deviceCode", command.getDeviceCode());
        sql.params().set("productId", command.getProductId());
        sql.params().set("commandCode", command.getCommandCode());
        sql.params().set("payloadJson", Strings.sBlank(command.getPayloadJson(), "{}"));
        sql.params().set("replyRequired", command.isReplyRequired());
        sql.params().set("deadlineAt", command.getDeadlineAt());
        sql.params().set("messageId", Strings.sBlank(dto.getMessageId(), command.getMessageId()));
        sql.params().set("messageTopic", Strings.sBlank(command.getMessageTopic(), ""));
        sql.params().set("queuedAt", command.getQueuedAt());
        sql.params().set("sentAt", resolveSentAt(command, dto));
        sql.params().set("finishedAt", dto.getFinishedAt());
        sql.params().set("responseJson", Strings.sBlank(dto.getResponseJson(), ""));
        sql.params().set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""));
        sql.params().set("status", status == null ? "" : status.name());
        sql.params().set("createdBy", Strings.sBlank(command.getCreatedBy(), "gateway"));
        sql.params().set("createdAt", command.getCreatedAt());
        sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"));
        sql.params().set("updatedAt", now);
        sql.params().set("delFlag", false);
        dao.execute(sql);
        return sql.getUpdateCount() > 0;
    }

    private boolean createMongoHistory(Device_command command, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null || command == null) {
            return false;
        }
        String productKey = resolveProductKey(command);
        String collection = archiveProperties.getMongoCollectionPrefix() + "_command_" + normalizeProductKey(productKey);
        String messageId = Strings.sBlank(dto.getMessageId(), command.getMessageId());
        if (Strings.isNotBlank(messageId) && mongoTemplate.exists(Query.query(Criteria.where("messageId").is(messageId)), collection)) {
            return true;
        }
        org.bson.Document document = new org.bson.Document();
        document.put("id", Strings.sBlank(command.getId(), ""));
        document.put("tenantId", command.getTenantId());
        document.put("productId", command.getProductId());
        document.put("productKey", productKey);
        document.put("deviceId", command.getDeviceId());
        document.put("deviceCode", command.getDeviceCode());
        document.put("commandCode", command.getCommandCode());
        document.put("payloadJson", Strings.sBlank(command.getPayloadJson(), "{}"));
        document.put("replyRequired", command.isReplyRequired());
        document.put("deadlineAt", command.getDeadlineAt());
        document.put("messageId", messageId);
        document.put("messageTopic", Strings.sBlank(command.getMessageTopic(), ""));
        document.put("queuedAt", command.getQueuedAt());
        document.put("sentAt", resolveSentAt(command, dto));
        document.put("finishedAt", dto.getFinishedAt());
        document.put("responseJson", Strings.sBlank(dto.getResponseJson(), ""));
        document.put("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""));
        document.put("status", status == null ? "" : status.name());
        document.put("createdBy", Strings.sBlank(command.getCreatedBy(), "gateway"));
        document.put("createdAt", command.getCreatedAt());
        document.put("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "gateway"));
        document.put("updatedAt", System.currentTimeMillis());
        document.put("delFlag", false);
        mongoTemplate.insert(document, collection);
        return true;
    }

    private boolean tableExists(String tableName) {
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> rs.next() ? rs.getString(1) : null);
        dao.execute(sql);
        return Strings.isNotBlank(sql.getString());
    }

    private String resolvePartitionTable(String baseTable, String productKey, Long ts) {
        if (!"device_command_log".equals(baseTable)) {
            return baseTable;
        }
        return schemaService.resolveHistoryTable(productKey, ts);
    }

    private List<String> resolveRelationalTables(String productKey, Long ts) {
        if (Strings.isNotBlank(productKey) && ts != null) {
            String table = resolvePartitionTable("device_command_log", productKey, ts);
            return Strings.isBlank(table) ? List.of() : List.of(table);
        }
        Set<String> tables = new java.util.LinkedHashSet<>();
        tables.addAll(queryTableNames("device_command_log_%"));
        return new ArrayList<>(tables);
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

    private String normalizeProductKey(String productKey) {
        return schemaService.normalizePartitionKey(productKey);
    }

    private String resolveProductKey(Device_command command) {
        if (command == null || Strings.isBlank(command.getDeviceId())) {
            return "";
        }
        Device_info device = dao.fetch(Device_info.class, Cnd.where("tenantId", "=", command.getTenantId())
                .and("id", "=", command.getDeviceId())
                .and("delFlag", "=", false));
        return device == null ? "" : Strings.sBlank(device.getProductKey(), "");
    }

    private DeviceCommandStatus resolveNextStatus(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        if (shouldKeepPending(command, dto)) {
            return DeviceCommandStatus.PENDING;
        }
        return dto.getStatus();
    }

    private boolean shouldKeepPending(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        return command != null
                && dto != null
                && dto.getStatus() == DeviceCommandStatus.FAILED
                && command.getStatus() == DeviceCommandStatus.PENDING;
    }

    private boolean shouldResetQueuedState(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        return shouldKeepPending(command, dto);
    }

    private boolean shouldDeleteCommand(Device_command command, DeviceCommandStatus status) {
        if (command == null || status == null) {
            return false;
        }
        if (status == DeviceCommandStatus.SENT) {
            return true;
        }
        if (status == DeviceCommandStatus.CANCELLED) {
            return true;
        }
        return status == DeviceCommandStatus.SUCCESS || status == DeviceCommandStatus.FAILED;
    }

    private Long resolveSentAt(Device_command command, DeviceCommandStatusUpdateDTO dto) {
        if (dto != null && dto.getSentAt() != null) {
            return dto.getSentAt();
        }
        return command == null ? null : command.getSentAt();
    }
}
