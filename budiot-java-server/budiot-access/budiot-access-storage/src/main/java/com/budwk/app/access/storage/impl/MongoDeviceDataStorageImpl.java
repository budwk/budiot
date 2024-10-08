package com.budwk.app.access.storage.impl;


import com.budwk.app.access.constants.StorageConstant;
import com.budwk.app.access.objects.dto.DeviceAttributeDTO;
import com.budwk.app.access.objects.dto.DeviceDataDTO;
import com.budwk.app.access.objects.query.DeviceDataQuery;
import com.budwk.app.access.storage.DeviceDataStorage;
import com.budwk.starter.mongodb.ZMongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wizzer.cn
 */
@IocBean
@Slf4j
public class MongoDeviceDataStorageImpl implements DeviceDataStorage {
    private final static String TABLE_PREFIX = "device_data_";

    @Inject
    private ZMongoDatabase zMongoDatabase;

    @Inject
    private PropertiesProxy conf;

    private JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder().objectIdConverter((value, writer) -> {
                writer.writeString(value.toString());
            }).dateTimeConverter((value, writer) -> {
                writer.writeNumber(Long.toString(value));
            }).decimal128Converter((value, writer) -> writer.writeString(value.toString()))
            .build();

    @Override
    public void save(DeviceDataDTO dataDTO, Map<String, Object> dataList) {
        String tableName = String.format("%s%s", TABLE_PREFIX, dataDTO.getProtocol_code().toLowerCase());
        MongoCollection<Document> collection = getCollection(tableName);
        dataList.put("ts", new Date(dataDTO.getTs()));
        Document meta = Lang.obj2map(dataDTO, Document.class);
        Document doc = new Document();
        doc.put("meta", meta);
        doc.putAll(dataList);
        collection.insertOne(doc);
    }

    @Override
    public NutMap list(DeviceDataQuery query) {
        String tableName = String.format("%s%s", TABLE_PREFIX, query.getProtocolCode().toLowerCase());
        MongoCollection<Document> collection = zMongoDatabase.getCollection(tableName);
        if (null == collection) {
            return NutMap.NEW().addv("total", 0).addv("list", Collections.emptyList());
        }
        NutMap nutMap = NutMap.NEW();
        Bson cnd = Filters.and(this.buildConditions(query));
        nutMap.addv("total", collection.countDocuments(cnd));
        FindIterable<Document> findIterable = collection.find(cnd);
        if (query.getPageNo() != null && query.getPageSize() != null) {
            findIterable.limit(query.getPageSize());
            findIterable.skip((query.getPageNo() - 1) * query.getPageSize());
        }
        findIterable.sort(Sorts.descending("ts"));
        MongoCursor<Document> cursor = findIterable.cursor();
        List<NutMap> data = new LinkedList<>();
        while (cursor.hasNext()) {
            NutMap map = Json.fromJson(NutMap.class, cursor.next().toJson(jsonWriterSettings));
            map.put("id", map.get("_id"));
            data.add(map);
        }
        nutMap.addv("list", data);
        return nutMap;
    }

    @Override
    public long count(DeviceDataQuery query) {
        String tableName = String.format("%s%s", TABLE_PREFIX, query.getProtocolCode().toLowerCase());
        MongoCollection<Document> collection = zMongoDatabase.getCollection(tableName);
        if (null == collection) {
            return 0L;
        }
        List<Bson> cnd = buildConditions(query);
        return collection.countDocuments(Filters.and(cnd));
    }

    @Override
    public void createTable(DeviceDataDTO deviceDTO, List<DeviceAttributeDTO> attributeDTOS) {

    }

    private synchronized MongoCollection<Document> getCollection(String table) {
        MongoCollection<Document> collection = zMongoDatabase.getCollection(table);
        if (null == collection) {
            log.debug("集合 {} 不存在, 重新创建", table);
            collection = crateCollection(table);
        }
        return collection;
    }

    private MongoCollection<Document> crateCollection(String cName) {
        CreateCollectionOptions options = new CreateCollectionOptions();
        TimeSeriesOptions timeSeriesOptions = new TimeSeriesOptions("ts");
        //时间序列的聚合粒度(数据库会将一个时间段的数据聚合存放, 这个参数影响性能, 不影响功能)
        timeSeriesOptions.granularity(TimeSeriesGranularity.MINUTES);
        timeSeriesOptions.metaField("meta");
        long expireDay = conf.getLong(StorageConstant.DATA_TTL_CONFIG_KEY, StorageConstant.DATA_TTL_CONFIG_DEFAULT);
        if (expireDay > 0) {
            options.expireAfter(3600 * 24 * expireDay, TimeUnit.SECONDS);
        }
        options.timeSeriesOptions(timeSeriesOptions);
        MongoCollection<Document> collection = zMongoDatabase.createCollection(cName, options);
        // 创建索引
        IndexOptions indexOptions = new IndexOptions().unique(false).background(true);
        collection.createIndexes(List.of(
                new IndexModel(Indexes.descending("ts"), indexOptions),
                new IndexModel(Indexes.ascending("meta.device_id"), indexOptions),
                new IndexModel(Indexes.ascending("meta.product_id"), indexOptions)
        ));
        return collection;
    }

    protected List<Bson> buildConditions(DeviceDataQuery query) {
        List<Bson> conditions = new LinkedList<>();
        conditions.add(Filters.eq("meta.device_id", query.getDeviceId()));
        if (query.getStartTime() != null) {
            conditions.add(Filters.gte("ts", new Date(query.getStartTime())));
        }
        if (query.getEndTime() != null) {
            conditions.add(Filters.lte("ts", new Date(query.getEndTime())));
        }
        if (Strings.isNotBlank(query.getIds())) {
            List<ObjectId> ids = Arrays.stream(query.getIds().split(",")).map(i -> new ObjectId(i)).collect(Collectors.toList());
            conditions.add(Filters.in("_id", ids));
        }
        return conditions;
    }

}
