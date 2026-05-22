package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgHistoryQueryDTO;
import com.budwk.sp.msg.entity.Msg_history;
import com.budwk.sp.msg.services.MsgHistoryService;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.pager.Pager;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.util.Daos;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MsgHistoryServiceImpl extends BaseServiceImpl<Msg_history> implements MsgHistoryService {
    private static final DateTimeFormatter TABLE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> ORDER_FIELDS = Set.of("createdAt", "sendAt", "receiver", "status", "channelType", "providerType");
    private static final String SELECT_COLUMNS = "id, tenantId, requestNo, channelId, templateId, channelType, providerType, bizType, receiver, receiverName, title, content, paramsJson, status, providerCode, providerMsg, providerRequestId, sendAt, successAt, failAt, createdBy, createdAt, updatedBy, updatedAt, delFlag";
    private final Map<String, Dao> ymDaos = new HashMap<>();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public MsgHistoryServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public void save(Msg_history history) {
        historyDao(history.getCreatedAt()).insert(history);
    }

    @Override
    public Pagination listPage(String tenantId, MsgHistoryQueryDTO dto) {
        long endTime = dto.getEndTime() == null ? System.currentTimeMillis() : dto.getEndTime();
        long beginTime = dto.getBeginTime() == null ? LocalDate.now().minusMonths(1).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : dto.getBeginTime();
        List<String> keys = resolveRangeKeys(beginTime, endTime);
        if (keys.isEmpty()) {
            return new Pagination(dto.getPageNo(), dto.getPageSize(), 0, new ArrayList<>(), Msg_history.class);
        }
        QueryContext context = buildContext(tenantId, dto, beginTime, endTime);
        String unionSql = buildUnionSql(keys, context.whereClause());
        String orderField = ORDER_FIELDS.contains(dto.getPageOrderName()) ? dto.getPageOrderName() : "createdAt";
        String orderBy = PageUtil.getOrder(dto.getPageOrderBy());
        if (!StringUtils.hasText(orderBy)) {
            orderBy = "desc";
        }
        Sql countSql = Sqls.create("SELECT COUNT(1) FROM (" + unionSql + ") t");
        Sql sql = Sqls.create("SELECT * FROM (" + unionSql + ") t ORDER BY " + orderField + " " + orderBy);
        context.params().forEach((key, value) -> {
            countSql.params().set(key, value);
            sql.params().set(key, value);
        });
        countSql.setCallback(Sqls.callback.integer());
        this.dao().execute(countSql);
        Pager pager = this.dao().createPager(dto.getPageNo(), dto.getPageSize());
        pager.setRecordCount(countSql.getInt());
        sql.setPager(pager);
        sql.setEntity(getEntity());
        sql.setCallback(Sqls.callback.entities());
        this.dao().execute(sql);
        List<Msg_history> histories = sql.getList(getEntityClass());
        this.fetchLinks(histories, "channel");
        return new Pagination(dto.getPageNo(), dto.getPageSize(), countSql.getInt(), histories, getEntityClass());
    }

    @Override
    public Msg_history fetchDetail(String id, Long createdAt, String tenantId) {
        if (!StringUtils.hasText(id) || createdAt == null) {
            return null;
        }
        return historyDao(createdAt).fetch(getEntityClass(), Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
    }

    public Dao historyDao() {
        return historyDao(System.currentTimeMillis());
    }

    public Dao historyDao(Long createdAt) {
        return historyDao(getMonthKey(createdAt == null ? System.currentTimeMillis() : createdAt));
    }

    public Dao historyDao(String key) {
        Dao dao = ymDaos.get(key);
        if (dao == null) {
            synchronized (this) {
                dao = ymDaos.get(key);
                if (dao == null) {
                    dao = Daos.ext(this.dao(), key);
                    dao.create(Msg_history.class, false);
                    ensureIndexes(getTableName(key));
                    ymDaos.put(key, dao);
                }
            }
        }
        return dao;
    }

    private QueryContext buildContext(String tenantId, MsgHistoryQueryDTO dto, Long beginTime, Long endTime) {
        StringBuilder where = new StringBuilder(" WHERE delFlag=@delFlag AND tenantId=@tenantId");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("delFlag", false);
        params.put("tenantId", tenantId);
        if (Strings.isNotBlank(dto.getChannelId())) {
            where.append(" AND channelId=@channelId");
            params.put("channelId", dto.getChannelId());
        }
        if (Strings.isNotBlank(dto.getChannelType())) {
            where.append(" AND channelType=@channelType");
            params.put("channelType", dto.getChannelType());
        }
        if (Strings.isNotBlank(dto.getProviderType())) {
            where.append(" AND providerType=@providerType");
            params.put("providerType", dto.getProviderType());
        }
        if (Strings.isNotBlank(dto.getStatus())) {
            where.append(" AND status=@status");
            params.put("status", dto.getStatus());
        }
        if (Strings.isNotBlank(dto.getReceiver())) {
            where.append(" AND receiver LIKE @receiver");
            params.put("receiver", "%" + dto.getReceiver() + "%");
        }
        if (Strings.isNotBlank(dto.getTitle())) {
            where.append(" AND title LIKE @title");
            params.put("title", "%" + dto.getTitle() + "%");
        }
        if (beginTime != null) {
            where.append(" AND createdAt>=@beginTime");
            params.put("beginTime", beginTime);
        }
        if (endTime != null) {
            where.append(" AND createdAt<=@endTime");
            params.put("endTime", endTime);
        }
        return new QueryContext(where.toString(), params);
    }

    private String buildUnionSql(List<String> keys, String whereClause) {
        List<String> sqls = new ArrayList<>();
        for (String key : keys) {
            sqls.add("SELECT " + SELECT_COLUMNS + " FROM " + getTableName(key) + whereClause);
        }
        return String.join(" UNION ALL ", sqls);
    }

    private List<String> resolveRangeKeys(Long beginTime, Long endTime) {
        long begin = beginTime == null ? System.currentTimeMillis() : beginTime;
        long end = endTime == null ? System.currentTimeMillis() : endTime;
        if (begin > end) {
            long tmp = begin;
            begin = end;
            end = tmp;
        }
        YearMonth startMonth = YearMonth.from(Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()));
        YearMonth endMonth = YearMonth.from(Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()));
        List<String> keys = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            String key = month.format(TABLE_SUFFIX);
            if (this.dao().exists(getTableName(key))) {
                keys.add(key);
            }
        }
        keys.sort(Comparator.reverseOrder());
        return keys;
    }

    private String getMonthKey(Long createdAt) {
        return YearMonth.from(Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())).format(TABLE_SUFFIX);
    }

    private String getTableName(String key) {
        return "msg_history_" + key;
    }

    private void ensureIndexes(String tableName) {
        createIndexIfAbsent(tableName + "_tenant_idx", tableName, "tenantId", "createdAt");
        createIndexIfAbsent(tableName + "_status_idx", tableName, "status", "createdAt");
        createIndexIfAbsent(tableName + "_receiver_idx", tableName, "receiver", "createdAt");
    }

    private void createIndexIfAbsent(String indexName, String tableName, String... fields) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        Sql sql = Sqls.create("CREATE INDEX " + indexName + " ON " + tableName + "(" + String.join(",", fields) + ")");
        this.dao().execute(sql);
    }

    private boolean indexExists(String tableName, String indexName) {
        final boolean[] exists = {false};
        this.dao().run(conn -> {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(conn.getCatalog(), conn.getSchema(), tableName, false, false)) {
                while (resultSet.next()) {
                    String current = resultSet.getString("INDEX_NAME");
                    if (current != null && indexName.equalsIgnoreCase(current)) {
                        exists[0] = true;
                        break;
                    }
                }
            }
        });
        return exists[0];
    }

    private record QueryContext(String whereClause, Map<String, Object> params) {
    }
}
