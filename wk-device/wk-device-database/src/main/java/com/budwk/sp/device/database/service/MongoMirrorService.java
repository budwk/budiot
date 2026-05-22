package com.budwk.sp.device.database.service;

import com.budwk.sp.device.database.config.DeviceDatabaseProperties;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.TimeSeriesGranularity;
import com.mongodb.client.model.TimeSeriesOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "wk.device.database-ext", name = "mongo-enabled", havingValue = "true")
public class MongoMirrorService {
    private static final String TIME_FIELD = "ts";
    private static final String META_FIELD = "meta";
    private final MongoTemplate mongoTemplate;
    private final DeviceDatabaseProperties properties;
    private final Set<String> collectionCache = ConcurrentHashMap.newKeySet();
    private final Set<String> plainCollectionWarnings = ConcurrentHashMap.newKeySet();

    public MongoMirrorService(MongoTemplate mongoTemplate, DeviceDatabaseProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    public void archiveRaw(Document document) {
        archive("raw", document, properties.getMessageRetentionDays());
    }

    public void archiveData(Document document) {
        archive("data", document, properties.getDataRetentionDays());
    }

    public void archiveEvent(Document document) {
        archive("event", document, properties.getEventRetentionDays());
    }

    public void archiveCommand(Document document) {
        archiveCommandInternal(document, properties.getCommandRetentionDays());
    }

    private void archive(String category, Document document, int retentionDays) {
        try {
            String collection = collection(category, String.valueOf(document.getOrDefault("productKey", "default")));
            prepareTimeSeriesDocument(document);
            ensureTimeSeriesCollection(collection, retentionDays);
            mongoTemplate.insert(document, collection);
        } catch (Exception e) {
            log.error("archive mongo {} failed", category, e);
        }
    }

    private void archiveCommandInternal(Document document, int retentionDays) {
        try {
            String collection = collection("command", String.valueOf(document.getOrDefault("productKey", "default")));
            prepareTimeSeriesDocument(document);
            ensureTimeSeriesCollection(collection, retentionDays);
            String messageId = String.valueOf(document.getOrDefault("messageId", ""));
            String tenantId = String.valueOf(document.getOrDefault("tenantId", ""));
            if (!messageId.isBlank() && mongoTemplate.exists(Query.query(Criteria.where("tenantId").is(tenantId).and("messageId").is(messageId)), collection)) {
                return;
            }
            mongoTemplate.insert(document, collection);
        } catch (Exception e) {
            log.error("archive mongo command failed", e);
        }
    }

    private void prepareTimeSeriesDocument(Document document) {
        long timestamp = resolveTimestamp(document);
        document.put(TIME_FIELD, new Date(timestamp));
        document.put(META_FIELD, new Document()
                .append("tenantId", String.valueOf(document.getOrDefault("tenantId", "")))
                .append("productId", String.valueOf(document.getOrDefault("productId", "")))
                .append("productKey", String.valueOf(document.getOrDefault("productKey", "")))
                .append("deviceId", String.valueOf(document.getOrDefault("deviceId", "")))
                .append("deviceCode", String.valueOf(document.getOrDefault("deviceCode", ""))));
    }

    private long resolveTimestamp(Document document) {
        Object deviceAt = document.get("deviceAt");
        if (deviceAt instanceof Number number && number.longValue() > 0L) {
            return number.longValue();
        }
        Object occurredAt = document.get("occurredAt");
        if (occurredAt instanceof Number number && number.longValue() > 0L) {
            return number.longValue();
        }
        Object createdAt = document.get("createdAt");
        if (createdAt instanceof Number number && number.longValue() > 0L) {
            return number.longValue();
        }
        return System.currentTimeMillis();
    }

    private void ensureTimeSeriesCollection(String collection, int retentionDays) {
        if (!collectionCache.add(collection)) {
            return;
        }
        Document info = mongoTemplate.getDb().listCollections().filter(new Document("name", collection)).first();
        if (info != null) {
            if (!isTimeSeriesCollection(info) && plainCollectionWarnings.add(collection)) {
                log.warn("Mongo collection {} 已存在且不是时序集合，无法自动转换；如需启用时序集合请先迁移并重建该集合", collection);
            }
            return;
        }
        CreateCollectionOptions options = new CreateCollectionOptions()
                .timeSeriesOptions(new TimeSeriesOptions(TIME_FIELD)
                        .metaField(META_FIELD)
                        .granularity(TimeSeriesGranularity.SECONDS))
                .expireAfter(retentionDays, TimeUnit.DAYS);
        mongoTemplate.getDb().createCollection(collection, options);
    }

    private boolean isTimeSeriesCollection(Document info) {
        Object options = info.get("options");
        if (!(options instanceof Document optionsDocument)) {
            return false;
        }
        return optionsDocument.get("timeseries") instanceof Document;
    }

    private String collection(String category, String productKey) {
        return properties.getMongoCollectionPrefix() + "_" + category + "_" + productKey.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
}
