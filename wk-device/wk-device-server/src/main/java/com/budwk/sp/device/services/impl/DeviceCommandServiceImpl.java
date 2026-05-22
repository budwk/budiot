package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.config.DeviceArchiveProperties;
import com.budwk.sp.device.dto.DeviceCommandDTO;
import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessagePublishResultDTO;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.entity.Device_command_log;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.services.DeviceCommandService;
import com.budwk.sp.device.services.DeviceInfoService;
import com.budwk.sp.device.services.DeviceProtocolService;
import com.budwk.sp.device.services.DeviceProductService;
import com.budwk.sp.device.support.DeviceCommandSchemaService;
import com.budwk.sp.device.services.support.DeviceArchiveMongoQueryService;
import com.budwk.sp.device.services.support.DeviceArchiveDefaultQueryService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Sqls;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceCommandServiceImpl extends BaseServiceImpl<Device_command> implements DeviceCommandService {
    private final DeviceInfoService deviceInfoService;
    private final DeviceProductService deviceProductService;
    private final DeviceProtocolService deviceProtocolService;
    private final DeviceMessageTemplate deviceMessageTemplate;
    private final DeviceArchiveProperties deviceArchiveProperties;
    private final DeviceArchiveDefaultQueryService relationalQueryService;
    private final DeviceArchiveMongoQueryService mongoQueryService;
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final DeviceCommandSchemaService schemaService;

    public DeviceCommandServiceImpl(Dao dao,
                                    DeviceInfoService deviceInfoService,
                                    DeviceProductService deviceProductService,
                                    DeviceProtocolService deviceProtocolService,
                                    DeviceMessageTemplate deviceMessageTemplate,
                                    DeviceArchiveProperties deviceArchiveProperties,
                                    DeviceArchiveDefaultQueryService relationalQueryService,
                                    DeviceArchiveMongoQueryService mongoQueryService,
                                    ObjectProvider<MongoTemplate> mongoTemplateProvider,
                                    DeviceCommandSchemaService schemaService) {
        super(dao);
        this.deviceInfoService = deviceInfoService;
        this.deviceProductService = deviceProductService;
        this.deviceProtocolService = deviceProtocolService;
        this.deviceMessageTemplate = deviceMessageTemplate;
        this.deviceArchiveProperties = deviceArchiveProperties;
        this.relationalQueryService = relationalQueryService;
        this.mongoQueryService = mongoQueryService;
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.schemaService = schemaService;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public List<Device_command> createCommands(DeviceCommandDTO dto, String operatorId, String tenantId) {
        List<String> targetIds = dto.getDeviceIds();
        if ((targetIds == null || targetIds.isEmpty()) && Strings.isNotBlank(dto.getDeviceId())) {
            targetIds = List.of(dto.getDeviceId());
        }
        if (targetIds == null || targetIds.isEmpty()) throw new BaseException("请选择下发设备");
        List<Device_info> devices = deviceInfoService.getDevicesByIds(targetIds, tenantId);
        if (devices.isEmpty()) throw new BaseException("未找到可下发的设备");
        Map<String, Device_product> productMap = deviceProductService.query(Cnd.where("tenantId", "=", tenantId).and("id", "in", devices.stream().map(Device_info::getProductId).distinct().toList()).and("delFlag", "=", false))
                .stream().collect(Collectors.toMap(Device_product::getId, Function.identity(), (a, b) -> a));
        List<Device_command> result = new ArrayList<>();
        for (Device_info device : devices) {
            Device_product product = productMap.get(device.getProductId());
            Device_command command = new Device_command();
            command.setTenantId(tenantId);
            command.setDeviceId(device.getId());
            command.setDeviceCode(device.getDeviceCode());
            command.setProductId(device.getProductId());
            command.setCommandCode(Strings.sNull(dto.getCommandCode()).trim());
            command.setPayloadJson(dto.getPayloadJson());
            command.setReplyRequired(dto.isReplyRequired());
            command.setDeadlineAt(dto.getDeadlineAt());
            command.setStatus(DeviceCommandStatus.PENDING);
            command.setCreatedBy(operatorId);
            command.setUpdatedBy(operatorId);
            this.insert(command);
            result.add(command);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Device_command retryCommand(String id, String tenantId, String operatorId) {
        Device_command command = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (command == null) throw new BaseException("指令不存在");
        if (command.getStatus() == DeviceCommandStatus.CANCELLED) throw new BaseException("已取消的指令不可重试");
        command.setStatus(DeviceCommandStatus.PENDING);
        command.setMessageId("");
        command.setMessageTopic("");
        command.setQueuedAt(null);
        command.setUpdatedBy(operatorId);
        this.updateIgnoreNull(command);
        return command;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void cancelCommand(String id, String tenantId, String operatorId) {
        Device_command command = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (command == null) throw new BaseException("指令不存在");
        DeviceCommandStatusUpdateDTO dto = new DeviceCommandStatusUpdateDTO();
        dto.setCommandId(command.getId());
        dto.setMessageId(command.getMessageId());
        dto.setStatus(DeviceCommandStatus.CANCELLED);
        dto.setErrorMessage("");
        dto.setResponseJson("");
        dto.setFinishedAt(System.currentTimeMillis());
        dto.setUpdatedBy(operatorId);
        updateCommandStatus(dto);
    }

    @Override
    public List<Device_command> listPending(String deviceId, String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("deviceId", "=", deviceId).and("status", "=", DeviceCommandStatus.PENDING).and("delFlag", "=", false).desc("createdAt"));
    }

    @Override
    public List<Device_command_log> listLogs(String deviceId, String tenantId) {
        Device_info device = deviceInfoService.getDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (deviceArchiveProperties.resolveCommandStorage()) {
            case MONGODB -> mongoQueryService.listCommandLogs(tenantId, deviceId, device.getProductKey(), 50);
            default -> relationalQueryService.listCommandLogs(tenantId, deviceId, device.getProductKey(), 50);
        };
    }

    @Override
    public Pagination pageLogs(String deviceId, String tenantId, int pageNo, int pageSize) {
        return pageLogs(deviceId, tenantId, null, null, pageNo, pageSize);
    }

    @Override
    public Pagination pageLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize) {
        List<Device_command_log> logs = listLogs(deviceId, tenantId, startAt, endAt, Integer.MAX_VALUE);
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        int total = logs.size();
        int fromIndex = Math.min((normalizedPageNo - 1) * normalizedPageSize, total);
        int toIndex = Math.min(fromIndex + normalizedPageSize, total);
        return new Pagination(
                normalizedPageNo,
                normalizedPageSize,
                total,
                total == 0 ? Collections.emptyList() : logs.subList(fromIndex, toIndex)
        );
    }

    @Override
    public Pagination pagePending(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId)
                .and("deviceId", "=", deviceId)
                .and("status", "=", DeviceCommandStatus.PENDING)
                .and("delFlag", "=", false);
        if (startAt != null) {
            cnd.and("createdAt", ">=", startAt);
        }
        if (endAt != null) {
            cnd.and("createdAt", "<=", endAt);
        }
        cnd.desc("createdAt");
        return this.listPage(Math.max(pageNo, 1), Math.max(pageSize, 1), cnd);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateCommandStatus(DeviceCommandStatusUpdateDTO dto) {
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
                .add("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"))
                .add("updatedAt", System.currentTimeMillis());
        if (shouldResetQueuedState(command, dto)) {
            chain.add("messageId", "");
            chain.add("messageTopic", "");
            chain.add("queuedAt", null);
        }
        dao().update(Device_command.class, chain, Cnd.where("id", "=", command.getId()));
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
        Device_info device = deviceInfoService.getDevice(command.getDeviceId(), command.getTenantId());
        if (device == null) {
            return;
        }
        boolean historyUpdated = switch (deviceArchiveProperties.resolveCommandStorage()) {
            case MONGODB -> updateMongoCommandLog(command, device, dto);
            default -> updateDefaultCommandLog(command, device, dto);
        };
        if (!historyUpdated) {
            historyUpdated = createHistory(command, device, dto, nextStatus);
        }
        if (historyUpdated && shouldDeleteCommand(command, nextStatus)) {
            this.delete(command.getId());
        }
    }

    private DeviceMessagePublishResultDTO publishCommand(Device_command command, Device_info device, Device_product product, String operatorId, String tenantId) {
        DeviceDownlinkCommandMessageDTO message = new DeviceDownlinkCommandMessageDTO();
        message.setCommandId(command.getId());
        message.setCommandLogId(command.getId());
        message.setTenantId(tenantId);
        message.setProductId(device.getProductId());
        message.setProductKey(device.getProductKey());
        message.setDeviceId(device.getId());
        message.setDeviceCode(device.getDeviceCode());
        message.setGatewayNodeId(product != null ? product.getGatewayNodeId() : "");
        message.setNetworkProtocol(product != null && product.getNetworkProtocol() != null ? product.getNetworkProtocol().getValue() : "");
        message.setCommandCode(command.getCommandCode());
        message.setPayloadJson(Strings.sBlank(command.getPayloadJson(), "{}"));
        message.setPayload(resolveEncodedPayload(command, device, product, tenantId));
        message.setReplyRequired(command.isReplyRequired());
        message.setDeadlineAt(command.getDeadlineAt());
        return deviceMessageTemplate.publishDownlinkCommand(message, operatorId);
    }

    private String resolveEncodedPayload(Device_command command, Device_info device, Device_product product, String tenantId) {
        if (product == null || Strings.isBlank(product.getProtocolId())) {
            return command.getPayloadJson();
        }
        return deviceProtocolService.encodeCommand(product.getProtocolId(), command.getCommandCode(), command.getPayloadJson(), device, tenantId);
    }

    private Device_command resolveCommand(DeviceCommandStatusUpdateDTO dto) {
        Device_command command = null;
        if (Strings.isNotBlank(dto.getCommandId())) {
            command = this.fetch(dto.getCommandId());
        }
        if (command == null && Strings.isNotBlank(dto.getMessageId())) {
            command = this.fetch(Cnd.where("messageId", "=", dto.getMessageId()).and("delFlag", "=", false));
        }
        return command;
    }

    private List<Device_command_log> listLogs(String deviceId, String tenantId, Long startAt, Long endAt, int limit) {
        Device_info device = deviceInfoService.getDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (deviceArchiveProperties.resolveCommandStorage()) {
            case MONGODB -> mongoQueryService.listCommandLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
            default -> relationalQueryService.listCommandLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
        };
    }

    private boolean updateDefaultCommandLog(Device_command command, Device_info device, DeviceCommandStatusUpdateDTO dto) {
        boolean updated = false;
        for (String table : resolveRelationalTables(device.getProductKey(), command.getQueuedAt() == null ? command.getCreatedAt() : command.getQueuedAt())) {
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
            sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"));
            sql.params().set("updatedAt", System.currentTimeMillis());
            sql.params().set("messageId", Strings.sBlank(dto.getMessageId(), command.getMessageId()));
            dao().execute(sql);
            updated = updated || sql.getUpdateCount() > 0;
        }
        return updated;
    }

    private boolean updateMongoCommandLog(Device_command command, Device_info device, DeviceCommandStatusUpdateDTO dto) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            return updateDefaultCommandLog(command, device, dto);
        }
        String collection = deviceArchiveProperties.getMongoCollectionPrefix() + "_command_" + normalizeProductKey(device.getProductKey());
        Query query = Query.query(Criteria.where("messageId").is(Strings.sBlank(dto.getMessageId(), command.getMessageId())));
        Update update = new Update()
                .set("status", dto.getStatus() == null ? "" : dto.getStatus().name())
                .set("finishedAt", dto.getFinishedAt())
                .set("responseJson", Strings.sBlank(dto.getResponseJson(), ""))
                .set("errorMessage", Strings.sBlank(dto.getErrorMessage(), ""))
                .set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"))
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
        if (deviceArchiveProperties.resolveCommandStorage() == com.budwk.sp.device.enums.DeviceArchiveStorageType.MONGODB) {
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
            sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"));
            sql.params().set("updatedAt", System.currentTimeMillis());
            sql.params().set("messageId", dto.getMessageId());
            dao().execute(sql);
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
                .set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"))
                .set("updatedAt", System.currentTimeMillis());
        if (dto.getSentAt() != null) {
            update.set("sentAt", dto.getSentAt());
        }
        String prefix = deviceArchiveProperties.getMongoCollectionPrefix() + "_command_";
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

    private boolean createHistory(Device_command command, Device_info device, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        return deviceArchiveProperties.resolveCommandStorage() == com.budwk.sp.device.enums.DeviceArchiveStorageType.MONGODB
                ? createMongoHistory(command, device, dto, status)
                : createRelationalHistory(command, device, dto, status);
    }

    private boolean createRelationalHistory(Device_command command, Device_info device, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        if (command == null || device == null) {
            return false;
        }
        Long createdAt = command.getQueuedAt() == null ? command.getCreatedAt() : command.getQueuedAt();
        String table = schemaService.resolveHistoryTable(device.getProductKey(), createdAt);
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
        sql.params().set("createdBy", Strings.sBlank(command.getCreatedBy(), "system"));
        sql.params().set("createdAt", command.getCreatedAt());
        sql.params().set("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"));
        sql.params().set("updatedAt", now);
        sql.params().set("delFlag", false);
        dao().execute(sql);
        return sql.getUpdateCount() > 0;
    }

    private boolean createMongoHistory(Device_command command, Device_info device, DeviceCommandStatusUpdateDTO dto, DeviceCommandStatus status) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null || command == null || device == null) {
            return false;
        }
        String collection = deviceArchiveProperties.getMongoCollectionPrefix() + "_command_" + normalizeProductKey(device.getProductKey());
        String messageId = Strings.sBlank(dto.getMessageId(), command.getMessageId());
        if (Strings.isNotBlank(messageId) && mongoTemplate.exists(Query.query(Criteria.where("messageId").is(messageId)), collection)) {
            return true;
        }
        org.bson.Document document = new org.bson.Document();
        document.put("id", Strings.sBlank(command.getId(), ""));
        document.put("tenantId", command.getTenantId());
        document.put("productId", command.getProductId());
        document.put("productKey", device.getProductKey());
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
        document.put("createdBy", Strings.sBlank(command.getCreatedBy(), "system"));
        document.put("createdAt", command.getCreatedAt());
        document.put("updatedBy", Strings.sBlank(dto.getUpdatedBy(), "system"));
        document.put("updatedAt", System.currentTimeMillis());
        document.put("delFlag", false);
        mongoTemplate.insert(document, collection);
        return true;
    }

    private boolean tableExists(String tableName) {
        Sql sql = Sqls.create("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema NOT IN ('pg_catalog','information_schema') AND table_name=@tableName");
        sql.params().set("tableName", tableName);
        sql.setCallback((conn, rs, sql1) -> rs.next() ? rs.getString(1) : null);
        dao().execute(sql);
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
        java.util.Set<String> tables = new java.util.LinkedHashSet<>();
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
        dao().execute(sql);
        return tableNames;
    }

    private String normalizeProductKey(String productKey) {
        return schemaService.normalizePartitionKey(productKey);
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
