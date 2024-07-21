package com.budwk.app.access.storage.impl;

import com.budwk.app.access.constants.StorageConstant;
import com.budwk.app.access.objects.dto.DeviceEventDataDTO;
import com.budwk.app.access.objects.query.DeviceEventDataQuery;
import com.budwk.app.access.storage.DeviceEventDataStorage;
import com.budwk.starter.mongodb.ZMongoClient;
import com.budwk.starter.mongodb.ZMongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wizzer.cn
 */
@IocBean
@Slf4j
public class MongoDeviceEventDataStorageImpl implements DeviceEventDataStorage {
    public static final String collection_name = "device_event_data_";
    @Inject
    private ZMongoClient zMongoClient;
    @Inject
    private PropertiesProxy conf;

    @Override
    public void save(DeviceEventDataDTO data) {
        Document document = new Document();
        data.setTs(System.currentTimeMillis());
        document.putAll(Lang.obj2map(data));
        getCollection(null).insertOne(document);
    }

    @Override
    public NutMap list(DeviceEventDataQuery query) {
        NutMap nutMap = NutMap.NEW();
        Bson cnd = Filters.and(this.buildConditions(query));
        LocalDate queryDate = null != query.getStartTime() ?
                LocalDate.ofInstant(Instant.ofEpochMilli(query.getStartTime()), ZoneId.systemDefault()) : LocalDate.now();
        nutMap.addv("total", count(query));
        FindIterable<Document> findIterable = getCollection(queryDate).find(cnd);
        if (query.getPageNo() != null && query.getPageSize() != null) {
            findIterable.limit(query.getPageSize());
            findIterable.skip((query.getPageNo() - 1) * query.getPageSize());
        }
        findIterable.sort(Sorts.descending("ts"));
        MongoCursor<Document> cursor = findIterable.cursor();
        List<Document> data = new LinkedList<>();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            data.add(doc);
        }
        nutMap.addv("list", data);
        return nutMap;
    }

    @Override
    public long count(DeviceEventDataQuery query) {
        Bson cnd = Filters.and(this.buildConditions(query));
        LocalDate queryDate = null != query.getStartTime() ?
                LocalDate.ofInstant(Instant.ofEpochMilli(query.getStartTime()), ZoneId.systemDefault()) : LocalDate.now();
        return getCollection(queryDate).countDocuments(cnd);
    }

    protected List<Bson> buildConditions(DeviceEventDataQuery query) {
        List<Bson> conditions = new LinkedList<>();
        if (Strings.isNotBlank(query.getProductId())) {
            conditions.add(Filters.eq("productId", query.getProductId()));
        }
        if (Strings.isNotBlank(query.getDeviceId())) {
            conditions.add(Filters.eq("deviceId", query.getDeviceId()));
        }
        if (query.getStartTime() != null) {
            conditions.add(Filters.gte("startTime", query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            conditions.add(Filters.lte("startTime", query.getEndTime()));
        }
        if (Strings.isNotBlank(query.getIds())) {
            List<ObjectId> ids = Arrays.stream(query.getIds().split(",")).map(i -> new ObjectId(i)).collect(Collectors.toList());
            conditions.add(Filters.in("_id", ids));
        }
        return conditions;
    }

    private MongoCollection<Document> getCollection(LocalDate queryDate) {
        //按日分表
        queryDate = null == queryDate ? LocalDate.now() : queryDate;
        String collectionName = String.format("%s%04d%02d%02d", collection_name, queryDate.getYear(), queryDate.getMonthValue(), queryDate.getDayOfMonth());
        ZMongoDatabase db = zMongoClient.db();
        MongoCollection<Document> collection;
        if (db.collectionExists(collectionName)) {
            // collectionExists 方法中存在listCollectionNames方法，会导致查询慢，所以在后面高并发的请求下可以改成定时任务提前创建好表
            collection = db.getNativeDB().getCollection(collectionName);
        } else {
            log.debug("集合 {} 不存在, 重新创建", collectionName);
            db.getNativeDB().createCollection(collectionName);
            collection = db.getNativeDB().getCollection(collectionName);
            collection.createIndexes(List.of(
                    new IndexModel(Indexes.descending("startTime", "endTime")),
                    new IndexModel(Indexes.hashed("deviceId")),
                    new IndexModel(Indexes.hashed("productId")),
                    new IndexModel(Indexes.descending("ts"),
                            new IndexOptions()
                                    .expireAfter(3600 * 24 * conf.getLong(StorageConstant.EVENT_DATA_TTL_CONFIG_KEY,
                                            StorageConstant.EVENT_DATA_TTL_CONFIG_DEFAULT), TimeUnit.SECONDS))
            ));
        }
        return collection;
    }
}
