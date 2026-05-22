package com.budwk.sp.sys.services.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.starter.log.model.SLogRecord;
import com.budwk.sp.sys.entity.Sys_log;
import com.budwk.sp.sys.services.SysLogService;
import org.nutz.dao.Dao;
import org.nutz.dao.Chain;
import org.nutz.dao.Sqls;
import org.nutz.dao.Cnd;
import org.nutz.dao.pager.Pager;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.util.Daos;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * 系统日志服务实现
 *
 * @author wizzer@qq.com
 */
@Service
public class SysLogServiceImpl extends BaseServiceImpl<Sys_log> implements SysLogService {
    private static final DateTimeFormatter TABLE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> ORDER_FIELDS = Set.of("createdAt", "executeTime", "loginname", "username", "type", "tag", "url", "ip");
    private static final String SELECT_COLUMNS = "id, tenantId, appId, userId, type, tag, msg, loginname, username, ip, url, method, os, browser, params, result, exception, executeTime, createdBy, createdAt, updatedBy, updatedAt, delFlag";
    private final Map<String, Dao> ymDaos = new HashMap<>();
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysLogServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public void save(SLogRecord record) {
        if (record == null) {
            return;
        }
        Sys_log log = new Sys_log();
        log.setTenantId(resolveTenantId(record));
        log.setAppId(resolveAppId(record));
        log.setUserId(resolveUserId(record));
        log.setType(record.getType());
        log.setTag(record.getTag());
        log.setMsg(record.getMsg());
        log.setLoginname(resolveLoginname(record));
        log.setUsername(resolveUsername(record));
        log.setIp(record.getIp());
        log.setUrl(record.getUrl());
        log.setMethod(record.getMethod());
        log.setOs(record.getOs());
        log.setBrowser(record.getBrowser());
        log.setParams(record.getParams());
        log.setResult(record.getResult());
        log.setException(record.getException());
        log.setExecuteTime(record.getExecuteTime());
        log.setCreatedAt(record.getCreatedAt());
        log.setCreatedBy(log.getUserId());
        log.setUpdatedAt(record.getCreatedAt());
        log.setUpdatedBy(log.getUserId());
        log.setDelFlag(false);
        logDao(record.getCreatedAt()).insert(log);
    }

    @Override
    public Pagination getLogPage(String appId, String type, String status, String loginname, String username,
                                 String tag, String msg, Long beginTime, Long endTime,
                                 int pageNo, int pageSize, String pageOrderName, String pageOrderBy) {
        return queryLogs(resolveQueryKeys(beginTime, endTime), appId, type, status, loginname, username, tag, msg,
                beginTime, endTime, null, pageNo, pageSize, pageOrderName, pageOrderBy);
    }

    @Override
    public Pagination getUserLogPage(String userId, int pageNo, int pageSize, String pageOrderName, String pageOrderBy) {
        long endTime = System.currentTimeMillis();
        long beginTime = LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return queryLogs(resolveRangeKeys(beginTime, endTime), null, null, null, null, null, null, null,
                beginTime, endTime, userId, pageNo, pageSize, pageOrderName, pageOrderBy);
    }

    @Override
    public boolean deleteLog(String id, Long createdAt) {
        if (!StringUtils.hasText(id) || createdAt == null) {
            return false;
        }
        return logDao(createdAt).update(getEntity(), Chain.make("delFlag", true), Cnd.where("id", "=", id).and("delFlag", "=", false)) > 0;
    }

    @Override
    public int clearLogs(String appId, String type, String status, String loginname, String username,
                         String tag, String msg, Long beginTime, Long endTime) {
        QueryContext context = buildQueryContext(appId, type, status, loginname, username, tag, msg, beginTime, endTime, null);
        List<String> keys = resolveQueryKeys(beginTime, endTime);
        if (keys.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String key : keys) {
            Sql sql = Sqls.create("UPDATE " + getTableName(key) + " SET delFlag=@deletedFlag" + context.whereClause());
            sql.params().set("deletedFlag", true);
            context.params().forEach((paramKey, value) -> sql.params().set(paramKey, value));
            this.dao().execute(sql);
            count += sql.getUpdateCount();
        }
        return count;
    }

    private Pagination queryLogs(List<String> keys,
                                 String appId, String type, String status,
                                 String loginname, String username, String tag, String msg,
                                 Long beginTime, Long endTime, String userId,
                                 int pageNo, int pageSize, String pageOrderName, String pageOrderBy) {
        if (keys.isEmpty()) {
            return new Pagination(pageNo, pageSize, 0, new ArrayList<>(), Sys_log.class);
        }
        QueryContext context = buildQueryContext(appId, type, status, loginname, username, tag, msg, beginTime, endTime, userId);
        String unionSql = buildUnionSql(keys, context.whereClause());
        String orderField = ORDER_FIELDS.contains(pageOrderName) ? pageOrderName : "createdAt";
        String orderBy = PageUtil.getOrder(pageOrderBy);
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
        Pager pager = this.dao().createPager(pageNo, pageSize);
        pager.setRecordCount(countSql.getInt());
        sql.setPager(pager);
        sql.setEntity(getEntity());
        sql.setCallback(Sqls.callback.entities());
        this.dao().execute(sql);
        return new Pagination(pageNo, pageSize, countSql.getInt(), sql.getList(getEntityClass()), getEntityClass());
    }

    private QueryContext buildQueryContext(String appId, String type, String status,
                                           String loginname, String username, String tag, String msg,
                                           Long beginTime, Long endTime, String userId) {
        StringBuilder where = new StringBuilder(" WHERE delFlag=@delFlag");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("delFlag", false);
        if (StringUtils.hasText(appId)) {
            where.append(" AND appId=@appId");
            params.put("appId", appId);
        }
        if (StringUtils.hasText(type)) {
            where.append(" AND type=@type");
            params.put("type", type);
        }
        if (StringUtils.hasText(loginname)) {
            where.append(" AND loginname LIKE @loginname");
            params.put("loginname", "%" + loginname + "%");
        }
        if (StringUtils.hasText(username)) {
            where.append(" AND username LIKE @username");
            params.put("username", "%" + username + "%");
        }
        if (StringUtils.hasText(tag)) {
            where.append(" AND tag LIKE @tag");
            params.put("tag", "%" + tag + "%");
        }
        if (StringUtils.hasText(msg)) {
            where.append(" AND msg LIKE @msg");
            params.put("msg", "%" + msg + "%");
        }
        if (StringUtils.hasText(userId)) {
            where.append(" AND userId=@userId");
            params.put("userId", userId);
        }
        if (beginTime != null) {
            where.append(" AND createdAt>=@beginTime");
            params.put("beginTime", beginTime);
        }
        if (endTime != null) {
            where.append(" AND createdAt<=@endTime");
            params.put("endTime", endTime);
        }
        if ("success".equalsIgnoreCase(status)) {
            where.append(" AND (exception IS NULL OR exception='')");
        } else if ("exception".equalsIgnoreCase(status)) {
            where.append(" AND exception IS NOT NULL AND exception<>''");
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

    private List<String> resolveQueryKeys(Long beginTime, Long endTime) {
        if (beginTime != null || endTime != null) {
            return resolveRangeKeys(beginTime, endTime);
        }
        List<String> keys = listAllShardKeys();
        if (keys.isEmpty()) {
            String currentKey = getMonthKey(System.currentTimeMillis());
            Dao dao = logDao(currentKey);
            if (dao != null) {
                keys.add(currentKey);
            }
        }
        return keys;
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

    private List<String> listAllShardKeys() {
        List<String> keys = new ArrayList<>();
        for (String key : ymDaos.keySet()) {
            if (this.dao().exists(getTableName(key))) {
                keys.add(key);
            }
        }
        keys.sort(Comparator.reverseOrder());
        return keys;
    }

    /**
     * 获取按月分表的Dao实例,即当前日期的dao实例
     *
     * @return
     */
    public Dao logDao() {
        return logDao(getMonthKey(System.currentTimeMillis()));
    }

    /**
     * 获取特定时间所属月份的Dao实例
     *
     * @param createdAt
     * @return
     */
    public Dao logDao(Long createdAt) {
        return logDao(getMonthKey(createdAt == null ? System.currentTimeMillis() : createdAt));
    }

    /**
     * 获取特定月份的Dao实例
     *
     * @param key
     * @return
     */
    public Dao logDao(String key) {
        Dao dao = ymDaos.get(key);
        if (dao == null) {
            synchronized (this) {
                dao = ymDaos.get(key);
                if (dao == null) {
                    dao = Daos.ext(this.dao(), key);
                    dao.create(Sys_log.class, false);
                    ensureIndexes(getTableName(key));
                    ymDaos.put(key, dao);
                }
            }
        }
        return dao;
    }

    private String getTableName(String key) {
        return "sys_log_" + key;
    }

    private String getMonthKey(Long createdAt) {
        Long time = createdAt == null ? System.currentTimeMillis() : createdAt;
        return YearMonth.from(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())).format(TABLE_SUFFIX);
    }

    private void ensureIndexes(String tableName) {
        createIndexIfAbsent(tableName + "_user_idx", tableName, "userId", "createdAt");
        createIndexIfAbsent(tableName + "_type_idx", tableName, "type", "createdAt");
        createIndexIfAbsent(tableName + "_app_idx", tableName, "appId", "createdAt");
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

    private String resolveTenantId(SLogRecord record) {
        if (record != null && StringUtils.hasText(record.getTenantId())) {
            return record.getTenantId();
        }
        SaSession session = getSessionSafe();
        if (session != null) {
            String tenantId = session.getString("tenantId");
            if (StringUtils.hasText(tenantId)) {
                return tenantId;
            }
        }
        return GlobalConstant.TENANT_ID_DEFAULT;
    }

    private String resolveAppId(SLogRecord record) {
        if (record != null && StringUtils.hasText(record.getAppId())) {
            return record.getAppId();
        }
        SaSession session = getSessionSafe();
        if (session != null && StringUtils.hasText(session.getString("appId"))) {
            return session.getString("appId");
        }
        return extractParam(record, "appId");
    }

    private String resolveUserId(SLogRecord record) {
        if (record != null && StringUtils.hasText(record.getUserId())) {
            return record.getUserId();
        }
        SaSession session = getSessionSafe();
        if (session != null) {
            Object loginId = session.getLoginId();
            if (loginId != null && StringUtils.hasText(String.valueOf(loginId))) {
                return String.valueOf(loginId);
            }
        }
        return firstNonBlank(extractParam(record, "userId"), extractParam(record, "id"));
    }

    private String resolveLoginname(SLogRecord record) {
        if (record != null && StringUtils.hasText(record.getLoginname())) {
            return record.getLoginname();
        }
        SaSession session = getSessionSafe();
        if (session != null && StringUtils.hasText(session.getString("loginname"))) {
            return session.getString("loginname");
        }
        return firstNonBlank(extractParam(record, "loginname"), extractParam(record, "mobile"));
    }

    private String resolveUsername(SLogRecord record) {
        if (record != null && StringUtils.hasText(record.getUsername())) {
            return record.getUsername();
        }
        SaSession session = getSessionSafe();
        if (session != null && StringUtils.hasText(session.getString("username"))) {
            return session.getString("username");
        }
        return extractParam(record, "username");
    }

    private String extractParam(SLogRecord record, String key) {
        if (record == null || record.getParamsMap() == null) {
            return "";
        }
        return findValue(record.getParamsMap(), key);
    }

    private String findValue(Object value, String key) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (key.equals(String.valueOf(entry.getKey()))) {
                    return Strings.sNull(entry.getValue());
                }
                String nested = findValue(entry.getValue(), key);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                String nested = findValue(item, key);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        }
        return "";
    }

    private SaSession getSessionSafe() {
        try {
            return StpUtil.getSession(false);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private record QueryContext(String whereClause, Map<String, Object> params) {
    }
}
