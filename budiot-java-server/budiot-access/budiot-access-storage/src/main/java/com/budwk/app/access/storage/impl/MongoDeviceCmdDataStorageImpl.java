package com.budwk.app.access.storage.impl;

import com.budwk.app.access.objects.dto.DeviceCmdDTO;
import com.budwk.app.access.objects.query.DeviceCmdDataQuery;
import com.budwk.app.access.storage.DeviceCmdDataStorage;
import com.budwk.starter.mongodb.ZMongoClient;
import com.budwk.starter.mongodb.ZMongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wizzer.cn
 */
@IocBean
@Slf4j
public class MongoDeviceCmdDataStorageImpl implements DeviceCmdDataStorage {
    public static final String collection_name = "device_cmd_data_";
    @Inject
    private ZMongoClient zMongoClient;
    @Inject
    private PropertiesProxy conf;

    public void create(int next) {
        LocalDate currentDate = LocalDate.now();
        ZMongoDatabase db = zMongoClient.db();
        for (int i = 0; i <= next; i++) {
            LocalDate futureDate = currentDate.plusYears(i);
            String collectionName = String.format("%s%04d", collection_name, futureDate.getYear());
            MongoCollection<Document> collection;
            if (!db.collectionExists(collectionName)) {
                log.debug("集合 {} 不存在, 重新创建", collectionName);
                db.getNativeDB().createCollection(collectionName);
                collection = db.getNativeDB().getCollection(collectionName);
                collection.createIndexes(List.of(
                        new IndexModel(Indexes.descending("createTime")),
                        new IndexModel(Indexes.hashed("deviceId")),
                        new IndexModel(Indexes.hashed("productId")),
                        new IndexModel(Indexes.descending("ts"))
                ));
            }
        }
    }

    @Override
    public void save(DeviceCmdDTO data) {
        Document document = new Document();
        data.setTs(System.currentTimeMillis());
        document.putAll(Lang.obj2map(data));
        getCollection(null).insertOne(document);
    }

    @Override
    public NutMap list(DeviceCmdDataQuery query) {
        Bson cnd = Filters.and(this.buildConditions(query));
        NutMap nutMap = NutMap.NEW();
        nutMap.addv("total", count(query));
        LocalDate queryDate = null != query.getStartTime() ?
                LocalDate.ofInstant(Instant.ofEpochMilli(query.getStartTime()), ZoneId.systemDefault()) : LocalDate.now();
        FindIterable<Document> findIterable = getCollection(queryDate).find(cnd);
        if (query.getPageNo() != null && query.getPageSize() != null) {
            findIterable.limit(query.getPageSize());
            findIterable.skip((query.getPageNo() - 1) * query.getPageSize());
        }
        findIterable.sort(Sorts.descending("ts"));
        MongoCursor<Document> cursor = findIterable.cursor();
        List<DeviceCmdDTO> data = new LinkedList<>();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            DeviceCmdDTO dto = Lang.map2Object(doc, DeviceCmdDTO.class);
            dto.setId(doc.getObjectId("_id").toHexString());
            data.add(dto);
        }
        nutMap.addv("list", data);
        return nutMap;
    }

    @Override
    public long count(DeviceCmdDataQuery query) {
        Bson cnd = Filters.and(this.buildConditions(query));
        LocalDate queryDate = null != query.getStartTime() ?
                LocalDate.ofInstant(Instant.ofEpochMilli(query.getStartTime()), ZoneId.systemDefault()) : LocalDate.now();
        return getCollection(queryDate).countDocuments(cnd);
    }

    protected List<Bson> buildConditions(DeviceCmdDataQuery query) {
        List<Bson> conditions = new LinkedList<>();
        if (Strings.isNotBlank(query.getProductId())) {
            conditions.add(Filters.eq("productId", query.getProductId()));
        }
        if (Strings.isNotBlank(query.getDeviceId())) {
            conditions.add(Filters.eq("deviceId", query.getDeviceId()));
        }
        if (query.getStartTime() != null) {
            conditions.add(Filters.gte("createTime", query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            conditions.add(Filters.lte("createTime", query.getEndTime()));
        }
        if (Strings.isNotBlank(query.getIds())) {
            List<ObjectId> ids = Arrays.stream(query.getIds().split(",")).map(i -> new ObjectId(i)).collect(Collectors.toList());
            conditions.add(Filters.in("_id", ids));
        }
        return conditions;
    }

    private MongoCollection<Document> getCollection(LocalDate queryDate) {
        //按年分表
        queryDate = null == queryDate ? LocalDate.now() : queryDate;
        String collectionName = String.format("%s%04d", collection_name, queryDate.getYear());
        ZMongoDatabase db = zMongoClient.db();
        MongoCollection<Document> collection = db.getNativeDB().getCollection(collectionName);
        return collection;
    }
}
