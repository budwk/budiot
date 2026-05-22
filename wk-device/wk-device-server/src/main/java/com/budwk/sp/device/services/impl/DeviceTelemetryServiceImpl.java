package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.config.DeviceArchiveProperties;
import com.budwk.sp.device.entity.Device_data_log;
import com.budwk.sp.device.entity.Device_event_log;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.device.services.DeviceTelemetryService;
import com.budwk.sp.device.services.support.DeviceArchiveDefaultQueryService;
import com.budwk.sp.device.services.support.DeviceArchiveMongoQueryService;
import com.budwk.sp.device.services.support.DeviceArchiveTdengineQueryService;
import com.budwk.sp.device.support.DeviceThingPropertySupport;
import com.budwk.sp.starter.common.page.Pagination;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceTelemetryServiceImpl implements DeviceTelemetryService {
    private final Dao dao;
    private final DeviceArchiveProperties properties;
    private final DeviceArchiveDefaultQueryService relationalQueryService;
    private final DeviceArchiveMongoQueryService mongoQueryService;
    private final DeviceArchiveTdengineQueryService tdengineQueryService;
    private final ObjectMapper objectMapper;

    public DeviceTelemetryServiceImpl(Dao dao,
                                      DeviceArchiveProperties properties,
                                      DeviceArchiveDefaultQueryService relationalQueryService,
                                      DeviceArchiveMongoQueryService mongoQueryService,
                                      DeviceArchiveTdengineQueryService tdengineQueryService,
                                      ObjectMapper objectMapper) {
        this.dao = dao;
        this.properties = properties;
        this.relationalQueryService = relationalQueryService;
        this.mongoQueryService = mongoQueryService;
        this.tdengineQueryService = tdengineQueryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Device_raw_log> listRawLogs(String deviceId, String tenantId, int limit) {
        Device_info device = fetchDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), limit);
            case TDENGINE -> tdengineQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), limit);
            default -> relationalQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), limit);
        };
    }

    @Override
    public List<Device_raw_log> listRawLogs(String tenantId, long startAt, long endAt) {
        return switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoQueryService.listRawLogsByRange(tenantId, startAt, endAt);
            case TDENGINE -> tdengineQueryService.listRawLogsByRange(tenantId, startAt, endAt);
            default -> relationalQueryService.listRawLogsByRange(tenantId, startAt, endAt);
        };
    }

    @Override
    public List<Map<String, Object>> listDataLogs(String deviceId, String tenantId, int limit) {
        return listDataLogs(deviceId, tenantId, null, null, limit);
    }

    @Override
    public List<Device_event_log> listEventLogs(String deviceId, String tenantId, int limit) {
        Device_info device = fetchDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (properties.resolveEventStorage()) {
            case MONGODB -> mongoQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), limit);
            case TDENGINE -> tdengineQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), limit);
            default -> relationalQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), limit);
        };
    }

    @Override
    public Pagination pageRawLogs(String deviceId, String tenantId, int pageNo, int pageSize) {
        return pageRawLogs(deviceId, tenantId, null, null, pageNo, pageSize);
    }

    @Override
    public Pagination pageRawLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize) {
        return paginate(listRawLogs(deviceId, tenantId, startAt, endAt, Integer.MAX_VALUE), pageNo, pageSize);
    }

    @Override
    public Pagination pageDataLogs(String deviceId, String tenantId, int pageNo, int pageSize) {
        return pageDataLogs(deviceId, tenantId, null, null, pageNo, pageSize);
    }

    @Override
    public Pagination pageDataLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize) {
        return paginate(listDataLogs(deviceId, tenantId, startAt, endAt, Integer.MAX_VALUE), pageNo, pageSize);
    }

    @Override
    public Pagination pageEventLogs(String deviceId, String tenantId, int pageNo, int pageSize) {
        return pageEventLogs(deviceId, tenantId, null, null, pageNo, pageSize);
    }

    @Override
    public Pagination pageEventLogs(String deviceId, String tenantId, Long startAt, Long endAt, int pageNo, int pageSize) {
        return paginate(listEventLogs(deviceId, tenantId, startAt, endAt, Integer.MAX_VALUE), pageNo, pageSize);
    }

    @Override
    public long countRawLogs(String tenantId, long startAt, long endAt) {
        return switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoQueryService.countRawLogs(tenantId, startAt, endAt);
            case TDENGINE -> tdengineQueryService.countRawLogs(tenantId, startAt, endAt);
            default -> relationalQueryService.countRawLogs(tenantId, startAt, endAt);
        };
    }

    @Override
    public long countHistoricalActiveDevices(String tenantId, long startAt, long endAt) {
        return switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoQueryService.countHistoricalActiveDevices(tenantId, startAt, endAt);
            case TDENGINE -> tdengineQueryService.countHistoricalActiveDevices(tenantId, startAt, endAt);
            default -> relationalQueryService.countHistoricalActiveDevices(tenantId, startAt, endAt);
        };
    }

    @Override
    public long countEventLogs(String tenantId, long startAt, long endAt, List<DeviceEventSourceType> sourceTypes) {
        return switch (properties.resolveEventStorage()) {
            case MONGODB -> mongoQueryService.countEventLogs(tenantId, startAt, endAt, sourceTypes);
            case TDENGINE -> tdengineQueryService.countEventLogs(tenantId, startAt, endAt, sourceTypes);
            default -> relationalQueryService.countEventLogs(tenantId, startAt, endAt, sourceTypes);
        };
    }

    private Device_info fetchDevice(String deviceId, String tenantId) {
        return dao.fetch(Device_info.class, Cnd.where("tenantId", "=", tenantId).and("id", "=", deviceId).and("delFlag", "=", false));
    }

    private Device_product fetchProduct(String productId, String tenantId) {
        if (Strings.isBlank(productId)) {
            return null;
        }
        return dao.fetch(Device_product.class, Cnd.where("tenantId", "=", tenantId).and("id", "=", productId).and("delFlag", "=", false));
    }

    private List<Device_raw_log> listRawLogs(String deviceId, String tenantId, Long startAt, Long endAt, int limit) {
        Device_info device = fetchDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (properties.resolveRawStorage()) {
            case MONGODB -> mongoQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
            case TDENGINE -> tdengineQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
            default -> relationalQueryService.listRawLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
        };
    }

    private List<Map<String, Object>> listDataLogs(String deviceId, String tenantId, Long startAt, Long endAt, int limit) {
        Device_info device = fetchDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        Device_product product = fetchProduct(device.getProductId(), tenantId);
        List<Map<String, Object>> propertyDefs = DeviceThingPropertySupport.parseProperties(product == null ? "" : product.getThingPropertyJson(), objectMapper);
        return switch (properties.resolveDataStorage()) {
            case MONGODB -> groupLegacyDataLogs(mongoQueryService.listDataLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit), propertyDefs);
            case TDENGINE -> groupLegacyDataLogs(tdengineQueryService.listDataLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit), propertyDefs);
            default -> normalizeRelationalRows(relationalQueryService.listDataRows(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit), propertyDefs);
        };
    }

    private List<Device_event_log> listEventLogs(String deviceId, String tenantId, Long startAt, Long endAt, int limit) {
        Device_info device = fetchDevice(deviceId, tenantId);
        if (device == null) {
            return List.of();
        }
        return switch (properties.resolveEventStorage()) {
            case MONGODB -> mongoQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
            case TDENGINE -> tdengineQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
            default -> relationalQueryService.listEventLogs(tenantId, deviceId, device.getProductKey(), startAt, endAt, limit);
        };
    }

    private List<Map<String, Object>> normalizeRelationalRows(List<Map<String, Object>> rows, List<Map<String, Object>> propertyDefs) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> {
            Map<String, Object> properties = buildPropertiesFromRow(row, propertyDefs);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", readString(row, "id"));
            item.put("messageId", readString(row, "messageid"));
            item.put("deviceAt", readLong(row.get("deviceat")));
            item.put("createdAt", readLong(row.get("createdat")));
            item.put("properties", properties);
            return item;
        }).filter(item -> {
            Object properties = item.get("properties");
            return properties instanceof Map<?, ?> propertyMap && !propertyMap.isEmpty();
        }).toList();
    }

    private Map<String, Object> buildPropertiesFromRow(Map<String, Object> row, List<Map<String, Object>> propertyDefs) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map<String, Object> propertyDef : propertyDefs) {
            String identifier = DeviceThingPropertySupport.readIdentifier(propertyDef);
            if (Strings.isBlank(identifier)) {
                continue;
            }
            String value = readString(row, DeviceThingPropertySupport.toColumnName(identifier).toLowerCase());
            if (Strings.isBlank(value)) {
                continue;
            }
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("identifier", identifier);
            property.put("name", DeviceThingPropertySupport.readName(propertyDef));
            property.put("unit", DeviceThingPropertySupport.readUnit(propertyDef));
            property.put("valueJson", value);
            properties.put(identifier, property);
        }
        return properties;
    }

    private List<Map<String, Object>> groupLegacyDataLogs(List<Device_data_log> logs, List<Map<String, Object>> propertyDefs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        Map<String, String> propertyNames = new LinkedHashMap<>();
        Map<String, String> propertyUnits = new LinkedHashMap<>();
        for (Map<String, Object> propertyDef : propertyDefs) {
            String identifier = DeviceThingPropertySupport.readIdentifier(propertyDef);
            if (Strings.isBlank(identifier)) {
                continue;
            }
            propertyNames.put(identifier, DeviceThingPropertySupport.readName(propertyDef));
            propertyUnits.put(identifier, DeviceThingPropertySupport.readUnit(propertyDef));
        }
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Device_data_log log : logs) {
            String groupKey = Strings.sBlank(log.getMessageId(), log.getId());
            Map<String, Object> row = grouped.computeIfAbsent(groupKey, key -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", key);
                item.put("messageId", key);
                item.put("deviceAt", log.getDeviceAt());
                item.put("createdAt", log.getCreatedAt());
                item.put("properties", new LinkedHashMap<String, Object>());
                return item;
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) row.get("properties");
            String identifier = Strings.sNull(log.getIdentifier()).trim();
            if (Strings.isBlank(identifier) || properties.containsKey(identifier)) {
                continue;
            }
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("identifier", identifier);
            property.put("name", Strings.sBlank(propertyNames.get(identifier), Strings.sNull(log.getName()).trim()));
            property.put("unit", Strings.sBlank(propertyUnits.get(identifier), Strings.sNull(log.getUnit()).trim()));
            property.put("valueJson", Strings.sNull(log.getValueJson()).trim());
            properties.put(identifier, property);
        }
        return grouped.values().stream().filter(item -> {
            Object properties = item.get("properties");
            return properties instanceof Map<?, ?> propertyMap && !propertyMap.isEmpty();
        }).toList();
    }

    private String readString(Map<String, Object> source, String key) {
        if (source == null || Strings.isBlank(key) || !source.containsKey(key) || source.get(key) == null) {
            return "";
        }
        return Strings.sNull(String.valueOf(source.get(key))).trim();
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private <T> Pagination paginate(List<T> list, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        int total = list == null ? 0 : list.size();
        int fromIndex = Math.min((normalizedPageNo - 1) * normalizedPageSize, total);
        int toIndex = Math.min(fromIndex + normalizedPageSize, total);
        List<T> rows = list == null ? Collections.emptyList() : list.subList(fromIndex, toIndex);
        return new Pagination(normalizedPageNo, normalizedPageSize, total, rows);
    }
}
