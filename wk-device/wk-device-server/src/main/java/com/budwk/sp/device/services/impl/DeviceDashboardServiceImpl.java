package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.enums.DeviceEventSourceType;
import com.budwk.sp.device.services.DeviceDashboardService;
import com.budwk.sp.device.services.DeviceTelemetryService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DeviceDashboardServiceImpl implements DeviceDashboardService {
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Dao dao;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceTelemetryService deviceTelemetryService;

    public DeviceDashboardServiceImpl(Dao dao, StringRedisTemplate stringRedisTemplate, DeviceTelemetryService deviceTelemetryService) {
        this.dao = dao;
        this.stringRedisTemplate = stringRedisTemplate;
        this.deviceTelemetryService = deviceTelemetryService;
    }

    @Override
    public Map<String, Object> getDashboardData(String tenantId, String rangeType, Long startAt, Long endAt) {
        String resolvedRangeType = Strings.sBlank(rangeType, "today").toLowerCase(Locale.ROOT);
        TimeRange current = resolveRange(resolvedRangeType, startAt, endAt);
        TimeRange previous = resolvePreviousRange(current);

        long totalDevices = countDeviceInfo(tenantId);
        long onlineDevices = countCurrentOnline(tenantId);
        long previousOnline = deviceTelemetryService.countHistoricalActiveDevices(tenantId, previous.startAt(), previous.endAt());
        long todayMessages = deviceTelemetryService.countRawLogs(tenantId, current.startAt(), current.endAt());
        long previousMessages = deviceTelemetryService.countRawLogs(tenantId, previous.startAt(), previous.endAt());
        long rawAlerts = deviceTelemetryService.countEventLogs(tenantId, current.startAt(), current.endAt(), List.of(DeviceEventSourceType.DEVICE));
        long ruleAlerts = deviceTelemetryService.countEventLogs(tenantId, current.startAt(), current.endAt(), List.of(DeviceEventSourceType.RULE));
        long totalAlerts = rawAlerts + ruleAlerts;
        long previousAlerts = deviceTelemetryService.countEventLogs(tenantId, previous.startAt(), previous.endAt(), List.of(DeviceEventSourceType.DEVICE, DeviceEventSourceType.RULE));

        Map<String, Object> map = new HashMap<>();
        map.put("cards", List.of(
                overviewCard("设备数量", totalDevices, "在线 " + onlineDevices, null, null, null, "/platform/iot/device"),
                overviewCard("当前在线", onlineDevices, "昨日在线 " + previousOnline, changeRate(onlineDevices, previousOnline), "danger", "DOWN", "/platform/iot/device"),
                overviewCard("今日设备通信量", todayMessages, "昨日上报数 " + previousMessages, changeRate(todayMessages, previousMessages), "success", "UP", "/platform/iot/dashboard"),
                alertCard(totalAlerts, rawAlerts, ruleAlerts, "/platform/iot/dashboard")
        ));
        map.put("vendorStats", queryVendorStats(tenantId, totalDevices));
        map.put("typeStats", queryTypeStats(tenantId, totalDevices));
        map.put("trend", buildTrend(tenantId, resolvedRangeType, current));
        return map;
    }

    private Map<String, Object> overviewCard(String title, long value, String subLabel, Double rate, String rateType, String trend, String target) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("value", value);
        map.put("subLabel", subLabel);
        map.put("changeRate", rate);
        map.put("rateType", rateType);
        map.put("trend", trend);
        map.put("target", target);
        return map;
    }

    private Map<String, Object> alertCard(long totalAlerts, long rawAlerts, long ruleAlerts, String target) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "今日告警数量");
        map.put("value", totalAlerts);
        map.put("rawAlerts", rawAlerts);
        map.put("ruleAlerts", ruleAlerts);
        map.put("target", target);
        return map;
    }

    private long countDeviceInfo(String tenantId) {
        return dao.count(Device_info.class, Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false));
    }

    private long countCurrentOnline(String tenantId) {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match("wk:device:runtime:" + tenantId + ":*").count(500).build();
        try (RedisConnection connection = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection();
             Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                Map<Object, Object> runtime = stringRedisTemplate.opsForHash().entries(key);
                if ("true".equals(String.valueOf(runtime.getOrDefault("online", "false")))) {
                    count++;
                }
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    private List<Map<String, Object>> queryVendorStats(String tenantId, long totalDevices) {
        List<Map<String, Object>> list = new ArrayList<>();
        Sql sql = Sqls.create("""
                select coalesce(v.name, '未设置厂家') as name, count(di.id) as deviceCount
                from device_info di
                left join device_product dp on di.productId = dp.id and dp.delFlag = false
                left join device_vendor v on dp.vendorId = v.id and v.delFlag = false
                where di.tenantId=@tenantId and di.delFlag=false
                group by v.name
                order by count(di.id) desc, name asc
                limit 5
                """);
        sql.params().set("tenantId", tenantId);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                long count = rs.getLong("deviceCount");
                Map<String, Object> item = new HashMap<>();
                item.put("name", rs.getString("name"));
                item.put("deviceCount", count);
                item.put("ratio", ratio(count, totalDevices));
                list.add(item);
            }
            return null;
        });
        dao.execute(sql);
        if (list.isEmpty()) {
            list.add(Map.of("name", "暂无设备数据", "deviceCount", 0, "ratio", 0D));
        }
        return list;
    }

    private List<Map<String, Object>> queryTypeStats(String tenantId, long totalDevices) {
        List<Map<String, Object>> list = new ArrayList<>();
        Sql sql = Sqls.create("""
                select coalesce(dc.name, '未分类') as name, count(di.id) as deviceCount
                from device_info di
                left join device_product dp on di.productId = dp.id and dp.delFlag = false
                left join device_category dc on dp.categoryId = dc.id and dc.delFlag = false
                where di.tenantId=@tenantId and di.delFlag=false
                group by dc.name
                order by count(di.id) desc, name asc
                limit 6
                """);
        sql.params().set("tenantId", tenantId);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                long count = rs.getLong("deviceCount");
                Map<String, Object> item = new HashMap<>();
                item.put("name", rs.getString("name"));
                item.put("value", count);
                item.put("ratio", ratio(count, totalDevices));
                list.add(item);
            }
            return null;
        });
        dao.execute(sql);
        if (list.isEmpty()) {
            list.add(Map.of("name", "暂无设备类型", "value", 0, "ratio", 0D));
        }
        return list;
    }

    private Map<String, Object> buildTrend(String tenantId, String rangeType, TimeRange range) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        List<Device_raw_log> logs = deviceTelemetryService.listRawLogs(tenantId, range.startAt(), range.endAt());
        Map<String, Long> bucketMap = new LinkedHashMap<>();
        ChronoUnit unit;
        int bucketCount;
        if ("week".equals(rangeType)) {
            unit = ChronoUnit.DAYS;
            bucketCount = 7;
        } else if ("month".equals(rangeType)) {
            unit = ChronoUnit.DAYS;
            bucketCount = (int) Math.max(1, Duration.ofMillis(range.endAt() - range.startAt()).toDays() + 1);
        } else if ("year".equals(rangeType)) {
            unit = ChronoUnit.MONTHS;
            bucketCount = 12;
        } else {
            unit = ChronoUnit.HOURS;
            bucketCount = 24;
        }
        ZonedDateTime cursor = Instant.ofEpochMilli(range.startAt()).atZone(ZONE_ID);
        for (int i = 0; i < bucketCount; i++) {
            String key = formatBucket(cursor, unit);
            bucketMap.put(key, 0L);
            labels.add(displayLabel(cursor, unit));
            cursor = cursor.plus(1, unit);
        }
        for (Device_raw_log log : logs) {
            String key = formatBucket(Instant.ofEpochMilli(log.getCreatedAt()).atZone(ZONE_ID), unit);
            bucketMap.computeIfPresent(key, (k, v) -> v + 1);
        }
        values.addAll(bucketMap.values());
        Map<String, Object> map = new HashMap<>();
        map.put("rangeType", rangeType);
        map.put("startAt", range.startAt());
        map.put("endAt", range.endAt());
        map.put("startLabel", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(range.startAt()).atZone(ZONE_ID)));
        map.put("endLabel", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(range.endAt()).atZone(ZONE_ID)));
        map.put("labels", labels);
        map.put("values", values);
        map.put("total", values.stream().mapToLong(Long::longValue).sum());
        return map;
    }

    private String formatBucket(ZonedDateTime time, ChronoUnit unit) {
        if (unit == ChronoUnit.MONTHS) return time.getYear() + "-" + String.format("%02d", time.getMonthValue());
        if (unit == ChronoUnit.DAYS) return time.toLocalDate().toString();
        return time.toLocalDate() + " " + String.format("%02d", time.getHour());
    }

    private String displayLabel(ZonedDateTime time, ChronoUnit unit) {
        if (unit == ChronoUnit.MONTHS) return String.format("%02d月", time.getMonthValue());
        if (unit == ChronoUnit.DAYS) return String.format("%02d-%02d", time.getMonthValue(), time.getDayOfMonth());
        return String.format("%02d:00", time.getHour());
    }

    private Double changeRate(long current, long previous) {
        if (previous <= 0) return current <= 0 ? 0D : 100D;
        return Math.round(((current - previous) * 10000D / previous)) / 100D;
    }

    private double ratio(long value, long total) {
        if (total <= 0 || value <= 0) return 0D;
        return Math.round(value * 10000D / total) / 100D;
    }

    private TimeRange resolveRange(String rangeType, Long startAt, Long endAt) {
        if (startAt != null && endAt != null && startAt <= endAt) return new TimeRange(startAt, endAt);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID);
        return switch (rangeType) {
            case "week" -> new TimeRange(now.minusDays(6).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), now.withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant().toEpochMilli());
            case "month" -> new TimeRange(now.minusDays(29).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), now.withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant().toEpochMilli());
            case "year" -> new TimeRange(now.minusMonths(11).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant().toEpochMilli());
            default -> new TimeRange(now.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), now.withHour(23).withMinute(59).withSecond(59).withNano(0).toInstant().toEpochMilli());
        };
    }

    private TimeRange resolvePreviousRange(TimeRange current) {
        long duration = current.endAt() - current.startAt();
        return new TimeRange(current.startAt() - duration - 1, current.startAt() - 1);
    }

    private record TimeRange(long startAt, long endAt) {}
}
