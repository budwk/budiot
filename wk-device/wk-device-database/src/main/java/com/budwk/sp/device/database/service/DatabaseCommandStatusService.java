package com.budwk.sp.device.database.service;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import com.budwk.sp.device.entity.Device_command;
import com.budwk.sp.device.enums.DeviceArchiveStorageType;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.support.DeviceCommandSchemaService;
import org.bson.Document;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCommandStatusService {
    private final Dao dao;
    private final DeviceDatabaseProperties properties;
    private final DefaultArchiveService defaultArchiveService;
    private final ObjectProvider<MongoMirrorService> mongoMirrorServiceProvider;
    private final DeviceCommandSchemaService schemaService;

    public DatabaseCommandStatusService(Dao dao,
                                        DeviceDatabaseProperties properties,
                                        DefaultArchiveService defaultArchiveService,
                                        ObjectProvider<MongoMirrorService> mongoMirrorServiceProvider,
                                        DeviceCommandSchemaService schemaService) {
        this.dao = dao;
        this.properties = properties;
        this.defaultArchiveService = defaultArchiveService;
        this.mongoMirrorServiceProvider = mongoMirrorServiceProvider;
        this.schemaService = schemaService;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void completeByMessageId(String tenantId, String productKey, String messageId, Long finishedAt, String responseJson, String updatedBy) {
        if (Strings.isBlank(tenantId) || Strings.isBlank(messageId)) {
            return;
        }
        schemaService.ensurePendingColumns("device_command");
        Device_command command = dao.fetch(Device_command.class, Cnd.where("tenantId", "=", tenantId)
                .and("messageId", "=", messageId)
                .and("delFlag", "=", false));
        if (command == null) {
            return;
        }
        long completedAt = finishedAt == null || finishedAt <= 0L ? System.currentTimeMillis() : finishedAt;
        String operator = Strings.sBlank(updatedBy, "database");
        String reply = Strings.sBlank(responseJson, "{}");
        Chain chain = Chain.make("status", DeviceCommandStatus.SUCCESS)
                .add("finishedAt", completedAt)
                .add("responseJson", reply)
                .add("errorMessage", "")
                .add("updatedBy", operator)
                .add("updatedAt", completedAt);
        dao.update(Device_command.class, chain, Cnd.where("id", "=", command.getId()));
        command.setStatus(DeviceCommandStatus.SUCCESS);
        command.setFinishedAt(completedAt);
        command.setResponseJson(reply);
        command.setErrorMessage("");
        archiveHistory(command, productKey, operator);
        deletePendingByMessageId(tenantId, messageId);
    }

    private void archiveHistory(Device_command command, String productKey, String updatedBy) {
        String resolvedProductKey = Strings.sBlank(productKey, "default");
        long createdAt = command.getCreatedAt() == null || command.getCreatedAt() <= 0L
                ? (command.getFinishedAt() == null ? System.currentTimeMillis() : command.getFinishedAt())
                : command.getCreatedAt();
        if (properties.resolveCommandStorage() == DeviceArchiveStorageType.MONGODB) {
            MongoMirrorService mongoMirrorService = mongoMirrorServiceProvider.getIfAvailable();
            if (mongoMirrorService != null) {
                Document document = new Document();
                document.put("id", Strings.sBlank(command.getId(), ""));
                document.put("tenantId", command.getTenantId());
                document.put("productId", command.getProductId());
                document.put("productKey", resolvedProductKey);
                document.put("deviceId", command.getDeviceId());
                document.put("deviceCode", command.getDeviceCode());
                document.put("commandCode", command.getCommandCode());
                document.put("payloadJson", Strings.sBlank(command.getPayloadJson(), "{}"));
                document.put("replyRequired", command.isReplyRequired());
                document.put("deadlineAt", command.getDeadlineAt());
                document.put("messageId", Strings.sBlank(command.getMessageId(), ""));
                document.put("messageTopic", Strings.sBlank(command.getMessageTopic(), ""));
                document.put("queuedAt", command.getQueuedAt());
                document.put("sentAt", command.getSentAt());
                document.put("finishedAt", command.getFinishedAt());
                document.put("responseJson", Strings.sBlank(command.getResponseJson(), "{}"));
                document.put("errorMessage", Strings.sBlank(command.getErrorMessage(), ""));
                document.put("status", command.getStatus() == null ? "" : command.getStatus().name());
                document.put("createdBy", Strings.sBlank(command.getCreatedBy(), "database"));
                document.put("createdAt", createdAt);
                document.put("updatedBy", updatedBy);
                document.put("updatedAt", command.getFinishedAt() == null ? System.currentTimeMillis() : command.getFinishedAt());
                document.put("delFlag", false);
                mongoMirrorService.archiveCommand(document);
                return;
            }
        }
        defaultArchiveService.archiveCommand(resolvedProductKey, command.getTenantId(), command.getProductId(), command.getDeviceId(), command.getDeviceCode(),
                command.getCommandCode(), Strings.sBlank(command.getPayloadJson(), "{}"), command.isReplyRequired(), command.getDeadlineAt(),
                Strings.sBlank(command.getMessageId(), ""), Strings.sBlank(command.getMessageTopic(), ""), command.getQueuedAt(), command.getSentAt(), command.getFinishedAt(),
                Strings.sBlank(command.getResponseJson(), "{}"), Strings.sBlank(command.getErrorMessage(), ""), command.getStatus() == null ? "" : command.getStatus().name(), createdAt);
    }

    private void deletePendingByMessageId(String tenantId, String messageId) {
        Sql sql = Sqls.create("DELETE FROM device_command WHERE tenantId=@tenantId AND messageId=@messageId");
        sql.params().set("tenantId", tenantId);
        sql.params().set("messageId", messageId);
        dao.execute(sql);
    }
}
