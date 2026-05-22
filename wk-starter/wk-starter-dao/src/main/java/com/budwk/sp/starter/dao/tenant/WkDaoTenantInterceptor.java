package com.budwk.sp.starter.dao.tenant;

import com.budwk.sp.starter.dao.config.WkDaoTenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Condition;
import org.nutz.dao.DaoException;
import org.nutz.dao.DaoInterceptor;
import org.nutz.dao.DaoInterceptorChain;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.impl.jdbc.NutPojo;
import org.nutz.dao.impl.sql.pojo.AbstractPItem;
import org.nutz.dao.sql.DaoStatement;
import org.nutz.dao.sql.PItem;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.util.Pojos;
import org.nutz.lang.Strings;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class WkDaoTenantInterceptor implements DaoInterceptor {
    private static final Pattern INSERT_VALUES_PATTERN = Pattern.compile(
            "(?is)^\\s*insert\\s+into\\s+([\\w\".]+)\\s*\\((.*?)\\)\\s*values\\s*\\((.*?)\\)(.*)$");
    private static final Pattern SQL_VAR_PATTERN = Pattern.compile("\\$(\\w+)");
    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "on", "where", "left", "right", "inner", "outer", "join", "group", "order", "limit", "having", "set");
    private static final Map<String, Pattern> TENANT_PREDICATE_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Pattern> INSERT_COLUMN_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Field POJO_ITEMS_FIELD = getAccessibleField(NutPojo.class, "items");
    private static final Field CONDITION_ITEM_CND_FIELD = getAccessibleField("org.nutz.dao.impl.sql.pojo.ConditionPItem", "cnd");
    private static final Field INSERT_BY_CHAIN_NAMES_FIELD = getAccessibleField("org.nutz.dao.impl.sql.pojo.InsertByChainPItem", "names");
    private static final Field INSERT_BY_CHAIN_VALUES_FIELD = getAccessibleField("org.nutz.dao.impl.sql.pojo.InsertByChainPItem", "values");
    private final WkDaoTenantProperties properties;
    private final WkDaoTenantProvider tenantProvider;
    private final Set<String> tenantTables;
    private final Set<String> tenantTablePrefixes;
    private final Set<String> ignoreTables;

    public WkDaoTenantInterceptor(WkDaoTenantProperties properties, WkDaoTenantProvider tenantProvider, Environment environment) {
        this.properties = properties;
        this.tenantProvider = tenantProvider;
        this.tenantTables = Collections.unmodifiableSet(scanTenantTables(environment));
        this.tenantTablePrefixes = Collections.unmodifiableSet(scanTenantTablePrefixes(environment));
        this.ignoreTables = buildIgnoreTables(properties.getIgnoreTables());
        log.debug("tenant sql interceptor loaded, tables={}, prefixes={}, ignoreTables={}", tenantTables, tenantTablePrefixes, ignoreTables);
    }

    @Override
    public void filter(DaoInterceptorChain chain) throws DaoException {
        if (!properties.isEnabled() || WkDaoTenantContext.isIgnoreTenant()) {
            chain.doChain();
            return;
        }
        String tenantId = tenantProvider == null ? null : Strings.trim(tenantProvider.getTenantId());
        if (Strings.isBlank(tenantId)) {
            chain.doChain();
            return;
        }
        for (DaoStatement statement : chain.getDaoStatements()) {
            this.applyTenant(statement, tenantId);
        }
        chain.doChain();
    }

    private void applyTenant(DaoStatement statement, String tenantId) {
        if (statement == null || statement.isCreate() || statement.isDrop() || statement.isAlter()) {
            return;
        }
        if (statement instanceof Pojo pojo) {
            if (statement.isInsert()) {
                applyTenantToInsertPojo(pojo, tenantId);
                return;
            }
            applyTenantToPojo(pojo, tenantId);
            return;
        }
        if (statement instanceof Sql sql) {
            if (statement.isInsert()) {
                applyTenantToInsertSql(sql, tenantId);
                return;
            }
            applyTenantToSql(sql, tenantId);
        }
    }

    private void applyTenantToInsertPojo(Pojo pojo, String tenantId) {
        Entity<?> entity = pojo.getEntity();
        if (entity == null) {
            return;
        }
        MappingField tenantField = entity.getField("tenantId");
        if (tenantField == null || shouldIgnoreTable(entity.getTableName())) {
            return;
        }
        Object obj = pojo.getOperatingObject();
        if (obj instanceof Chain chain) {
            setTenantOnChain(chain, tenantField, tenantId);
            patchInsertByChainItem(pojo, tenantField, tenantId);
        } else if (obj instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stringMap = (Map<String, Object>) map;
            stringMap.put(tenantField.getName(), tenantId);
        } else if (obj != null) {
            tenantField.setValue(obj, tenantId);
        }
    }

    private void applyTenantToPojo(Pojo pojo, String tenantId) {
        Entity<?> entity = pojo.getEntity();
        if (entity == null) {
            return;
        }
        MappingField tenantField = entity.getField("tenantId");
        if (tenantField == null || shouldIgnoreTable(entity.getTableName())) {
            return;
        }
        if (mergeTenantIntoConditionItem(pojo, entity, tenantField, tenantId)) {
            return;
        }
        String prepared = Strings.sNull(pojo.toPreparedStatement());
        if (containsTenantPredicate(prepared, tenantField.getColumnName())) {
            return;
        }
        int clauseItemIndex = findTailClauseItemIndex(pojo, entity);
        AbstractPItem tenantCondition = (AbstractPItem) Pojos.Items.cndColumn(tenantField, tenantId);
        if (clauseItemIndex < 0) {
            if (hasWhere(prepared)) {
                tenantCondition.setTop(false);
                pojo.append(Pojos.Items.wrap(" AND "), tenantCondition);
                return;
            }
            pojo.append(tenantCondition);
            return;
        }
        String prefixSql = buildPojoSql(pojo, entity, clauseItemIndex);
        if (hasWhere(prefixSql)) {
            tenantCondition.setTop(false);
            insertItems(pojo, clauseItemIndex, Pojos.Items.wrap(" AND "), tenantCondition);
            return;
        }
        insertItems(pojo, clauseItemIndex, tenantCondition);
    }

    private boolean mergeTenantIntoConditionItem(Pojo pojo, Entity<?> entity, MappingField tenantField, String tenantId) {
        for (PItem item : getPojoItems(pojo)) {
            if (item == null) {
                continue;
            }
            if (item instanceof Cnd cnd) {
                if (containsTenantPredicate(cnd.toSql(entity), tenantField.getColumnName())) {
                    return true;
                }
                cnd.and(tenantField.getName(), "=", tenantId);
                return true;
            }
            if (item.getClass().getName().equals("org.nutz.dao.impl.sql.pojo.ConditionPItem")) {
                Cnd cnd = extractCondition(item);
                if (cnd == null) {
                    continue;
                }
                if (containsTenantPredicate(cnd.toSql(entity), tenantField.getColumnName())) {
                    return true;
                }
                cnd.and(tenantField.getName(), "=", tenantId);
                return true;
            }
        }
        return false;
    }

    private void applyTenantToSql(Sql sql, String tenantId) {
        String sourceSql = resolveSourceSqlVars(sql);
        if (Strings.isBlank(sourceSql)
                || containsTenantPredicate(sourceSql, "tenantId")) {
            if (!Strings.sNull(sql.getSourceSql()).equals(sourceSql)) {
                sql.setSourceSql(sourceSql);
            }
            return;
        }
        String rewritten = rewriteSql(sourceSql, tenantId);
        if (!sourceSql.equals(rewritten)) {
            sql.setSourceSql(rewritten);
        } else if (!Strings.sNull(sql.getSourceSql()).equals(sourceSql)) {
            sql.setSourceSql(sourceSql);
        }
    }

    private void applyTenantToInsertSql(Sql sql, String tenantId) {
        String sourceSql = resolveSourceSqlVars(sql);
        if (Strings.isBlank(sourceSql)
                || containsTenantInsertColumn(sourceSql, "tenantId")) {
            if (!Strings.sNull(sql.getSourceSql()).equals(sourceSql)) {
                sql.setSourceSql(sourceSql);
            }
            return;
        }
        Matcher matcher = INSERT_VALUES_PATTERN.matcher(sourceSql);
        if (!matcher.matches()) {
            if (!Strings.sNull(sql.getSourceSql()).equals(sourceSql)) {
                sql.setSourceSql(sourceSql);
            }
            return;
        }
        String tableName = normalizeTableName(matcher.group(1));
        if (!isTenantTable(tableName) || shouldIgnoreTable(tableName)) {
            if (!Strings.sNull(sql.getSourceSql()).equals(sourceSql)) {
                sql.setSourceSql(sourceSql);
            }
            return;
        }
        String columns = matcher.group(2).trim();
        String values = matcher.group(3).trim();
        String tail = matcher.group(4) == null ? "" : matcher.group(4);
        String rewritten = "INSERT INTO " + matcher.group(1).trim()
                + " (" + columns + ", tenantId) VALUES (" + values + ", '" + tenantId.replace("'", "''") + "')"
                + tail;
        sql.setSourceSql(rewritten);
    }

    private String resolveSourceSqlVars(Sql sql) {
        String sourceSql = Strings.sNull(sql.getSourceSql());
        if (Strings.isBlank(sourceSql) || sourceSql.indexOf('$') < 0) {
            return sourceSql;
        }
        Matcher matcher = SQL_VAR_PATTERN.matcher(sourceSql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = sql.vars().get(varName);
            String replacement = formatSqlVarValue(sql, value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String formatSqlVarValue(Sql sql, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof PItem item) {
            StringBuilder sb = new StringBuilder();
            item.joinSql(sql.getEntity(), sb);
            return sb.toString();
        }
        if (value instanceof Condition condition) {
            return " " + Pojos.formatCondition(sql.getEntity(), condition);
        }
        return Strings.sNull(value);
    }

    private String rewriteSql(String sourceSql, String tenantId) {
        String countWrappedSql = rewriteCountWrappedSql(sourceSql, tenantId);
        if (countWrappedSql != null) {
            return countWrappedSql;
        }
        String sqlWithNested = rewriteNestedQueries(sourceSql, tenantId);
        List<String> unionParts = splitTopLevelUnion(sqlWithNested);
        if (unionParts.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String part : unionParts) {
                if (sb.length() > 0) {
                    sb.append(part.startsWith("UNION") ? " " : "");
                }
                if (part.startsWith("UNION")) {
                    int index = part.indexOf(' ');
                    if (index > -1) {
                        sb.append(part, 0, index + 1);
                        sb.append(rewriteSingleSql(part.substring(index + 1), tenantId));
                    } else {
                        sb.append(part);
                    }
                } else {
                    sb.append(rewriteSingleSql(part, tenantId));
                }
            }
            return sb.toString();
        }
        return rewriteSingleSql(sqlWithNested, tenantId);
    }

    private String rewriteCountWrappedSql(String sourceSql, String tenantId) {
        String sql = Strings.sNull(sourceSql);
        String trimmedLower = sql.trim().toLowerCase(Locale.ROOT);
        if (!trimmedLower.startsWith("select count(")) {
            return null;
        }
        int fromIndex = findTopLevelKeyword(sql, "from");
        if (fromIndex < 0) {
            return null;
        }
        int subqueryStart = fromIndex + 4;
        while (subqueryStart < sql.length() && Character.isWhitespace(sql.charAt(subqueryStart))) {
            subqueryStart++;
        }
        if (subqueryStart >= sql.length() || sql.charAt(subqueryStart) != '(') {
            return null;
        }
        int subqueryEnd = findMatchingParenthesis(sql, subqueryStart);
        if (subqueryEnd < 0) {
            return null;
        }
        String innerSql = sql.substring(subqueryStart + 1, subqueryEnd);
        if (!startsWithSqlStatement(innerSql.trim())) {
            return null;
        }
        return sql.substring(0, subqueryStart + 1)
                + rewriteSql(innerSql, tenantId)
                + sql.substring(subqueryEnd);
    }

    private String rewriteNestedQueries(String sourceSql, String tenantId) {
        String sql = Strings.sNull(sourceSql);
        StringBuilder sb = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                sb.append(ch);
                continue;
            }
            if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                sb.append(ch);
                continue;
            }
            if (singleQuote || doubleQuote || ch != '(') {
                sb.append(ch);
                continue;
            }
            int end = findMatchingParenthesis(sql, i);
            if (end < 0) {
                sb.append(ch);
                continue;
            }
            String inner = sql.substring(i + 1, end);
            String trimmed = inner.trim();
            if (startsWithSqlStatement(trimmed)) {
                sb.append('(').append(rewriteSql(inner, tenantId)).append(')');
            } else {
                sb.append('(').append(rewriteNestedQueries(inner, tenantId)).append(')');
            }
            i = end;
        }
        return sb.toString();
    }

    private int findMatchingParenthesis(String sql, int start) {
        int depth = 0;
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = start; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                continue;
            }
            if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                continue;
            }
            if (singleQuote || doubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean startsWithSqlStatement(String sql) {
        if (Strings.isBlank(sql)) {
            return false;
        }
        String trimmed = sql.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("select")
                || trimmed.startsWith("update")
                || trimmed.startsWith("delete");
    }

    private String rewriteSingleSql(String sourceSql, String tenantId) {
        String sql = Strings.sNull(sourceSql);
        String lower = sql.toLowerCase(Locale.ROOT).trim();
        List<TableRef> refs = new ArrayList<>();
        if (lower.startsWith("select")) {
            refs.addAll(findSelectRefs(sql));
        } else if (lower.startsWith("update")) {
            TableRef updateRef = findUpdateRef(sql);
            if (updateRef != null) {
                refs.add(updateRef);
            }
        } else if (lower.startsWith("delete")) {
            TableRef deleteRef = findDeleteRef(sql);
            if (deleteRef != null) {
                refs.add(deleteRef);
            }
            refs.addAll(findJoinRefs(sql));
        } else {
            return sql;
        }
        List<String> conditions = new ArrayList<>();
        for (TableRef ref : refs) {
            if (!isTenantTable(ref.tableName) || shouldIgnoreTable(ref.tableName)) {
                continue;
            }
            String columnRef = Strings.isBlank(ref.alias) ? ref.tableName + ".tenantId" : ref.alias + ".tenantId";
            if (containsTenantPredicate(sql, columnRef) || containsTenantPredicate(sql, ref.tableName + ".tenantId")) {
                continue;
            }
            conditions.add(columnRef + " = '" + tenantId.replace("'", "''") + "'");
        }
        if (conditions.isEmpty()) {
            return sql;
        }
        int insertIndex = findClauseInsertIndex(sql);
        String conditionSql = String.join(" AND ", conditions);
        String prefix = rtrim(sql.substring(0, insertIndex));
        String suffix = ltrim(sql.substring(insertIndex));
        StringBuilder rewritten = new StringBuilder(prefix)
                .append(hasWhere(prefix) ? " AND " : " WHERE ")
                .append(conditionSql);
        if (Strings.isNotBlank(suffix)) {
            rewritten.append(' ').append(suffix);
        }
        return rewritten.toString();
    }

    private List<TableRef> findSelectRefs(String sql) {
        List<TableRef> refs = findRefsByKeyword(sql, "from");
        refs.addAll(findJoinRefs(sql));
        return refs;
    }

    private List<TableRef> findJoinRefs(String sql) {
        return findRefsByKeyword(sql, "join");
    }

    private TableRef findUpdateRef(String sql) {
        List<TableRef> refs = findRefsByKeyword(sql, "update");
        return refs.isEmpty() ? null : refs.get(0);
    }

    private TableRef findDeleteRef(String sql) {
        List<TableRef> refs = findRefsByKeyword(sql, "from");
        return refs.isEmpty() ? null : refs.get(0);
    }

    private List<TableRef> findRefsByKeyword(String sql, String keyword) {
        String lower = sql.toLowerCase(Locale.ROOT);
        List<TableRef> refs = new ArrayList<>();
        int depth = 0;
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                continue;
            }
            if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                continue;
            }
            if (singleQuote || doubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(depth - 1, 0);
                continue;
            }
            if (depth != 0 || !matchKeyword(lower, i, keyword)) {
                continue;
            }
            int pos = i + keyword.length();
            while (pos < sql.length() && Character.isWhitespace(sql.charAt(pos))) {
                pos++;
            }
            if (pos >= sql.length() || sql.charAt(pos) == '(') {
                continue;
            }
            String tableName = readIdentifier(sql, pos);
            if (Strings.isBlank(tableName)) {
                continue;
            }
            pos += tableName.length();
            while (pos < sql.length() && Character.isWhitespace(sql.charAt(pos))) {
                pos++;
            }
            if (matchKeyword(lower, pos, "as")) {
                pos += 2;
                while (pos < sql.length() && Character.isWhitespace(sql.charAt(pos))) {
                    pos++;
                }
            }
            String alias = readIdentifier(sql, pos);
            if (isReservedKeyword(alias)) {
                alias = "";
            }
            refs.add(new TableRef(normalizeTableName(tableName), Strings.sBlank(alias, normalizeTableName(tableName))));
        }
        return refs;
    }

    private int findClauseInsertIndex(String sql) {
        int groupIndex = findTopLevelKeyword(sql, "group by");
        int havingIndex = findTopLevelKeyword(sql, "having");
        int orderIndex = findTopLevelKeyword(sql, "order by");
        int limitIndex = findTopLevelKeyword(sql, "limit");
        int offsetIndex = findTopLevelKeyword(sql, "offset");
        int idx = sql.length();
        for (int value : new int[]{groupIndex, havingIndex, orderIndex, limitIndex, offsetIndex}) {
            if (value >= 0 && value < idx) {
                idx = value;
            }
        }
        return idx;
    }

    private int findTopLevelKeyword(String sql, String keyword) {
        String lower = sql.toLowerCase(Locale.ROOT);
        int depth = 0;
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = 0; i <= lower.length() - keyword.length(); i++) {
            char ch = lower.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                continue;
            }
            if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                continue;
            }
            if (singleQuote || doubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(depth - 1, 0);
                continue;
            }
            if (depth == 0 && matchKeyword(lower, i, keyword)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitTopLevelUnion(String sql) {
        List<String> parts = new ArrayList<>();
        String lower = sql.toLowerCase(Locale.ROOT);
        int depth = 0;
        boolean singleQuote = false;
        boolean doubleQuote = false;
        int start = 0;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
                continue;
            }
            if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
                continue;
            }
            if (singleQuote || doubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(depth - 1, 0);
                continue;
            }
            if (depth == 0 && matchKeyword(lower, i, "union")) {
                parts.add(sql.substring(start, i));
                int next = i + 5;
                if (matchKeyword(lower, next, "all")) {
                    next += 3;
                    parts.add("UNION ALL");
                } else {
                    parts.add("UNION");
                }
                start = next;
            }
        }
        if (start == 0) {
            return Collections.singletonList(sql);
        }
        parts.add(sql.substring(start));
        return parts;
    }

    private boolean matchKeyword(String text, int index, String keyword) {
        if (index < 0 || index + keyword.length() > text.length()) {
            return false;
        }
        if (!text.regionMatches(true, index, keyword, 0, keyword.length())) {
            return false;
        }
        char before = index > 0 ? text.charAt(index - 1) : ' ';
        char after = index + keyword.length() < text.length() ? text.charAt(index + keyword.length()) : ' ';
        return !Character.isLetterOrDigit(before) && before != '_' && !Character.isLetterOrDigit(after) && after != '_';
    }

    private boolean hasWhere(String sql) {
        return findTopLevelKeyword(sql, "where") >= 0;
    }

    private int findTailClauseItemIndex(Pojo pojo, Entity<?> entity) {
        List<PItem> items = getPojoItems(pojo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            int start = sb.length();
            PItem item = items.get(i);
            if (item != null) {
                item.joinSql(entity, sb);
            }
            int clausePos = findFirstTailClauseIndex(sb.toString());
            if (clausePos >= start) {
                return i;
            }
        }
        return -1;
    }

    private int findFirstTailClauseIndex(String sql) {
        int groupIndex = findTopLevelKeyword(sql, "group by");
        int havingIndex = findTopLevelKeyword(sql, "having");
        int orderIndex = findTopLevelKeyword(sql, "order by");
        int limitIndex = findTopLevelKeyword(sql, "limit");
        int offsetIndex = findTopLevelKeyword(sql, "offset");
        int idx = Integer.MAX_VALUE;
        for (int value : new int[]{groupIndex, havingIndex, orderIndex, limitIndex, offsetIndex}) {
            if (value >= 0 && value < idx) {
                idx = value;
            }
        }
        return idx == Integer.MAX_VALUE ? -1 : idx;
    }

    private String buildPojoSql(Pojo pojo, Entity<?> entity, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        List<PItem> items = getPojoItems(pojo);
        for (int i = 0; i < endExclusive && i < items.size(); i++) {
            PItem item = items.get(i);
            if (item != null) {
                item.joinSql(entity, sb);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<PItem> getPojoItems(Pojo pojo) {
        if (POJO_ITEMS_FIELD != null) {
            try {
                Object value = POJO_ITEMS_FIELD.get(pojo);
                if (value instanceof List<?> list) {
                    return (List<PItem>) list;
                }
            } catch (IllegalAccessException e) {
                log.debug("read pojo items failed for {}", pojo.getClass().getName(), e);
            }
        }
        List<PItem> items = new ArrayList<>();
        for (int index = 0; ; index++) {
            try {
                items.add(pojo.getItem(index));
            } catch (IndexOutOfBoundsException e) {
                return items;
            }
        }
    }

    private void insertItems(Pojo pojo, int index, PItem... newItems) {
        List<PItem> items = getPojoItems(pojo);
        if (index < 0 || index > items.size()) {
            index = items.size();
        }
        for (int i = 0; i < newItems.length; i++) {
            PItem item = newItems[i];
            if (item != null) {
                item.setPojo(pojo);
                items.add(index + i, item);
            }
        }
    }

    private boolean containsTenantPredicate(String sql, String columnRef) {
        if (Strings.isBlank(sql) || Strings.isBlank(columnRef)) {
            return false;
        }
        return TENANT_PREDICATE_PATTERN_CACHE
                .computeIfAbsent(columnRef, this::buildTenantPredicatePattern)
                .matcher(sql)
                .find();
    }

    private boolean containsTenantInsertColumn(String sql, String columnName) {
        if (Strings.isBlank(sql) || Strings.isBlank(columnName)) {
            return false;
        }
        Matcher matcher = INSERT_VALUES_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            return false;
        }
        String columns = matcher.group(2);
        return INSERT_COLUMN_PATTERN_CACHE
                .computeIfAbsent(columnName, key -> Pattern.compile("(?i)\\b" + Pattern.quote(key) + "\\b"))
                .matcher(columns)
                .find();
    }

    private boolean isReservedKeyword(String text) {
        if (Strings.isBlank(text)) {
            return true;
        }
        String value = text.toLowerCase(Locale.ROOT);
        return RESERVED_KEYWORDS.contains(value);
    }

    private String readIdentifier(String sql, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '"') {
                sb.append(ch);
                continue;
            }
            break;
        }
        return sb.toString();
    }

    private boolean isTenantTable(String tableName) {
        String normalized = normalizeTableName(tableName);
        if (tenantTables.contains(normalized)) {
            return true;
        }
        for (String prefix : tenantTablePrefixes) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIgnoreTable(String tableName) {
        return ignoreTables.contains(normalizeTableName(tableName));
    }

    private Set<String> scanTenantTables(Environment environment) {
        Set<String> tables = new LinkedHashSet<>();
        scanTenantMetadata(environment, tables, null);
        return tables;
    }

    private Set<String> scanTenantTablePrefixes(Environment environment) {
        Set<String> prefixes = new LinkedHashSet<>();
        scanTenantMetadata(environment, null, prefixes);
        return prefixes;
    }

    private void scanTenantMetadata(Environment environment, Set<String> tables, Set<String> prefixes) {
        List<String> packages = Binder.get(environment)
                .bind("wk.database.table.package", Bindable.listOf(String.class))
                .orElse(Collections.emptyList());
        if (packages.isEmpty()) {
            return;
        }
        org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider scanner =
                new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(org.nutz.dao.entity.annotation.Table.class));
        for (String pkg : packages) {
            if (Strings.isBlank(pkg)) {
                continue;
            }
            scanner.findCandidateComponents(pkg).forEach(bean -> {
                try {
                    Class<?> type = ClassUtils.forName(bean.getBeanClassName(), getClass().getClassLoader());
                    Field tenantField = findField(type, "tenantId");
                    org.nutz.dao.entity.annotation.Table table = type.getAnnotation(org.nutz.dao.entity.annotation.Table.class);
                    if (tenantField != null && table != null && Strings.isNotBlank(table.value())) {
                        String normalized = normalizeTableName(table.value());
                        if (normalized.contains("${")) {
                            if (prefixes != null) {
                                prefixes.add(normalized.substring(0, normalized.indexOf("${")));
                            }
                        } else if (tables != null) {
                            tables.add(normalized);
                        }
                    }
                } catch (Throwable e) {
                    log.warn("scan tenant table failed: {}", bean.getBeanClassName(), e);
                }
            });
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Set<String> buildIgnoreTables(List<String> tables) {
        Set<String> result = new LinkedHashSet<>();
        if (tables == null) {
            return result;
        }
        for (String table : tables) {
            if (Strings.isNotBlank(table)) {
                result.add(normalizeTableName(table));
            }
        }
        return result;
    }

    private String normalizeTableName(String tableName) {
        String value = Strings.sNull(tableName).trim().replace("\"", "");
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex > -1) {
            value = value.substring(dotIndex + 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String ltrim(String value) {
        return value == null ? "" : value.replaceFirst("^\\s+", "");
    }

    private String rtrim(String value) {
        return value == null ? "" : value.replaceFirst("\\s+$", "");
    }

    private void setTenantOnChain(Chain chain, MappingField tenantField, String tenantId) {
        Chain head = chain.head();
        Chain current = head;
        while (current != null) {
            if (tenantField.getName().equalsIgnoreCase(current.name())
                    || tenantField.getColumnName().equalsIgnoreCase(current.name())) {
                current.value(tenantId);
                return;
            }
            if (current.next() == null) {
                break;
            }
            current = current.next();
        }
        head.add(tenantField.getName(), tenantId);
    }

    private void patchInsertByChainItem(Pojo pojo, MappingField tenantField, String tenantId) {
        if (INSERT_BY_CHAIN_NAMES_FIELD == null || INSERT_BY_CHAIN_VALUES_FIELD == null) {
            return;
        }
        for (PItem item : getPojoItems(pojo)) {
            if (item == null || !"org.nutz.dao.impl.sql.pojo.InsertByChainPItem".equals(item.getClass().getName())) {
                continue;
            }
            try {
                String[] names = (String[]) INSERT_BY_CHAIN_NAMES_FIELD.get(item);
                Object[] values = (Object[]) INSERT_BY_CHAIN_VALUES_FIELD.get(item);
                for (int j = 0; j < names.length; j++) {
                    if (tenantField.getName().equalsIgnoreCase(names[j])
                            || tenantField.getColumnName().equalsIgnoreCase(names[j])) {
                        values[j] = tenantId;
                        INSERT_BY_CHAIN_VALUES_FIELD.set(item, values);
                        return;
                    }
                }
                String[] newNames = Arrays.copyOf(names, names.length + 1);
                Object[] newValues = Arrays.copyOf(values, values.length + 1);
                newNames[newNames.length - 1] = tenantField.getName();
                newValues[newValues.length - 1] = tenantId;
                INSERT_BY_CHAIN_NAMES_FIELD.set(item, newNames);
                INSERT_BY_CHAIN_VALUES_FIELD.set(item, newValues);
                return;
            } catch (IllegalAccessException e) {
                log.debug("patch insert chain item skipped for {}", item.getClass().getName(), e);
            }
        }
    }

    private Cnd extractCondition(PItem item) {
        if (CONDITION_ITEM_CND_FIELD == null) {
            return null;
        }
        try {
            Object cndObj = CONDITION_ITEM_CND_FIELD.get(item);
            return cndObj instanceof Cnd cnd ? cnd : null;
        } catch (IllegalAccessException e) {
            log.debug("merge tenant condition skipped for item {}", item.getClass().getName(), e);
            return null;
        }
    }

    private Pattern buildTenantPredicatePattern(String columnRef) {
        return Pattern.compile("(?i)\\b" + Pattern.quote(columnRef)
                + "\\b\\s*(=|<>|!=|in\\s*\\(|not\\s+in\\s*\\(|is\\s+null|is\\s+not\\s+null|like\\b)");
    }

    private static Field getAccessibleField(Class<?> type, String fieldName) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Field getAccessibleField(String className, String fieldName) {
        try {
            return getAccessibleField(Class.forName(className), fieldName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private record TableRef(String tableName, String alias) {
    }
}
