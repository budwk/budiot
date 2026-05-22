package com.budwk.sp.starter.log.aspect;

import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.log.config.WkStarterLogProperties;
import com.budwk.sp.starter.log.model.SLogRecord;
import com.budwk.sp.starter.log.providers.ISysLogProvider;
import com.budwk.sp.starter.log.util.UserAgentInfo;
import com.budwk.sp.starter.log.util.UserAgentParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * SLog 切面
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Aspect
public class SLogAspect {

    private final ObjectProvider<ObjectMapper> objectMapperProvider;
    private final ObjectProvider<ISysLogProvider> logProvider;
    private final WkStarterLogProperties properties;

    public SLogAspect(ObjectProvider<ObjectMapper> objectMapperProvider,
                      ObjectProvider<ISysLogProvider> logProvider,
                      WkStarterLogProperties properties) {
        this.objectMapperProvider = objectMapperProvider;
        this.logProvider = logProvider;
        this.properties = properties;
    }

    @Around("@annotation(com.budwk.sp.starter.log.annotation.SLog) || @within(com.budwk.sp.starter.log.annotation.SLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        SLog slog = resolveSLog(point);
        if (slog == null || !properties.isEnabled()) {
            return point.proceed();
        }
        long start = System.currentTimeMillis();
        Method method = resolveMethod(point);
        HttpRequestInfo requestInfo = getRequestInfo();
        Map<String, Object> paramsMap = buildParamsMap((MethodSignature) point.getSignature(), point.getArgs());
        UserContext userContext = resolveUserContext(paramsMap);
        Object result = null;
        Throwable throwable = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            ISysLogProvider provider = logProvider.getIfAvailable();
            if (provider != null) {
                try {
                    provider.save(buildRecord(point, slog, method, requestInfo, paramsMap, userContext, result, throwable, start));
                } catch (Exception e) {
                    log.error("record slog error", e);
                }
            }
        }
    }

    private SLogRecord buildRecord(ProceedingJoinPoint point, SLog slog, Method method,
                                   HttpRequestInfo requestInfo, Map<String, Object> paramsMap,
                                   UserContext userContext, Object result, Throwable throwable, long start) {
        String params = slog.saveParams() ? truncate(writeJson(paramsMap), properties.getMaxParamsLength()) : null;
        String resultText = slog.saveResult() ? truncate(resolveResultText(result), properties.getMaxResultLength()) : null;
        String exception = throwable == null ? null : truncate(formatException(throwable), properties.getMaxExceptionLength());
        UserAgentInfo userAgentInfo = UserAgentParser.parse(requestInfo.userAgent());
        long now = System.currentTimeMillis();
        return SLogRecord.builder()
                .createdAt(now)
                .tenantId(userContext.tenantId())
                .appId(userContext.appId())
                .userId(userContext.userId())
                .loginname(userContext.loginname())
                .username(userContext.username())
                .type(resolveType(slog, method, requestInfo.requestMethod(), requestInfo.uri()))
                .tag(resolveTag(slog, point))
                .msg(resolveMsg(slog, method))
                .url(requestInfo.uri())
                .method(method.getDeclaringClass().getName() + "#" + method.getName())
                .ip(requestInfo.ip())
                .browser(userAgentInfo.getBrowser())
                .os(userAgentInfo.getOs())
                .params(params)
                .paramsMap(paramsMap)
                .result(resultText)
                .exception(exception)
                .executeTime(now - start)
                .build();
    }

    private UserContext resolveUserContext(Map<String, Object> paramsMap) {
        String tenantId = firstNonBlank(getSaSessionValue("tenantId"), findValue(paramsMap, "tenantId"));
        String appId = firstNonBlank(getSaSessionValue("appId"), findValue(paramsMap, "appId"));
        String userId = firstNonBlank(getSaLoginId(), findValue(paramsMap, "userId"), findValue(paramsMap, "id"));
        String loginname = firstNonBlank(getSaSessionValue("loginname"), findValue(paramsMap, "loginname"), findValue(paramsMap, "mobile"));
        String username = firstNonBlank(getSaSessionValue("username"), findValue(paramsMap, "username"));
        return new UserContext(tenantId, appId, userId, loginname, username);
    }

    private Method resolveMethod(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return AopUtils.getMostSpecificMethod(signature.getMethod(), point.getTarget().getClass());
    }

    private SLog resolveSLog(ProceedingJoinPoint point) {
        Method method = resolveMethod(point);
        SLog slog = method.getAnnotation(SLog.class);
        if (slog != null) {
            return slog;
        }
        return point.getTarget().getClass().getAnnotation(SLog.class);
    }

    private String resolveTag(SLog slog, ProceedingJoinPoint point) {
        if (StringUtils.hasText(slog.tag())) {
            return slog.tag();
        }
        String classTag = readAnnotationValue(point.getTarget().getClass().getAnnotations(), "io.swagger.v3.oas.annotations.tags.Tag", "name");
        if (StringUtils.hasText(classTag)) {
            return classTag;
        }
        return point.getTarget().getClass().getSimpleName();
    }

    private String resolveMsg(SLog slog, Method method) {
        if (StringUtils.hasText(slog.msg())) {
            return slog.msg();
        }
        String summary = readAnnotationValue(method.getAnnotations(), "io.swagger.v3.oas.annotations.Operation", "summary");
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        return method.getName();
    }

    private String resolveType(SLog slog, Method method, String requestMethod, String uri) {
        if (StringUtils.hasText(slog.type())) {
            return slog.type().toUpperCase(Locale.ROOT);
        }
        String summary = readAnnotationValue(method.getAnnotations(), "io.swagger.v3.oas.annotations.Operation", "summary");
        summary = summary == null ? "" : summary;
        if (summary.contains("登录") || uri.endsWith("/login")) {
            return "LOGIN";
        }
        if (summary.contains("退出") || summary.contains("登出") || uri.endsWith("/logout")) {
            return "LOGOUT";
        }
        if (summary.contains("删除") || summary.contains("清空") || RequestMethod.DELETE.name().equalsIgnoreCase(requestMethod)) {
            return "DELETE";
        }
        if (summary.contains("新增") || summary.contains("创建") || summary.contains("导入")) {
            return "CREATE";
        }
        if (summary.contains("修改") || summary.contains("更新") || summary.contains("启用") || summary.contains("禁用")) {
            return "UPDATE";
        }
        if (summary.contains("查询") || summary.contains("获取") || summary.contains("搜索") || summary.contains("检索") || summary.contains("列表") || summary.contains("导出")) {
            return "QUERY";
        }
        if (RequestMethod.GET.name().equalsIgnoreCase(requestMethod) || uri.endsWith("/list") || uri.endsWith("/data")) {
            return "QUERY";
        }
        if (RequestMethod.POST.name().equalsIgnoreCase(requestMethod)) {
            return "CREATE";
        }
        if (RequestMethod.PUT.name().equalsIgnoreCase(requestMethod) || RequestMethod.PATCH.name().equalsIgnoreCase(requestMethod)) {
            return "UPDATE";
        }
        return "OTHER";
    }

    private Map<String, Object> buildParamsMap(MethodSignature signature, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        String[] names = signature.getParameterNames();
        if (names == null || args == null) {
            return map;
        }
        for (int i = 0; i < names.length && i < args.length; i++) {
            if (skipArg(args[i])) {
                continue;
            }
            map.put(names[i], sanitizeValue(names[i], args[i]));
        }
        return map;
    }

    private Object sanitizeValue(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitive(fieldName)) {
            return "******";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                copy.put(key, sanitizeValue(key, entry.getValue()));
            }
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>();
            for (Object item : collection) {
                list.add(sanitizeValue(fieldName, item));
            }
            return list;
        }
        if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                list.add(sanitizeValue(fieldName, Array.get(value, i)));
            }
            return list;
        }
        if (isSimpleValue(value)) {
            return value;
        }
        try {
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
            if (objectMapper != null) {
                Map<String, Object> converted = objectMapper.convertValue(value, new TypeReference<>() {
                });
                return sanitizeValue(fieldName, converted);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return truncate(String.valueOf(value), properties.getMaxParamsLength());
    }

    private boolean isSimpleValue(Object value) {
        Class<?> clazz = value.getClass();
        return ClassUtils.isPrimitiveOrWrapper(clazz)
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof Date
                || value instanceof UUID
                || value instanceof Temporal;
    }

    private boolean skipArg(Object value) {
        if (value == null) {
            return false;
        }
        return value instanceof ServletRequest
                || value instanceof ServletResponse
                || value instanceof InputStream
                || value instanceof OutputStream
                || "org.springframework.validation.BindingResult".equals(value.getClass().getName());
    }

    private String resolveResultText(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof Result<?> data) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", data.getCode());
            map.put("msg", data.getMsg());
            return writeJson(map);
        }
        return writeJson(sanitizeValue("result", result));
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
            if (objectMapper != null) {
                return objectMapper.writeValueAsString(value);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return String.valueOf(value);
    }

    private String getSaLoginId() {
        try {
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            Object loginId = stpUtilClass.getMethod("getLoginIdDefaultNull").invoke(null);
            return loginId == null ? "" : String.valueOf(loginId);
        } catch (Exception e) {
            return "";
        }
    }

    private String getSaSessionValue(String key) {
        try {
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            Object session = stpUtilClass.getMethod("getSession", boolean.class).invoke(null, false);
            if (session == null) {
                return "";
            }
            Object value = session.getClass().getMethod("getString", String.class).invoke(session, key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private String findValue(Object value, String key) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (key.equals(String.valueOf(entry.getKey()))) {
                    return entry.getValue() == null ? "" : String.valueOf(entry.getValue());
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String formatException(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private boolean isSensitive(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String lower = fieldName.toLowerCase(Locale.ROOT);
        for (String field : properties.getSensitiveFields()) {
            if (lower.contains(field.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private HttpRequestInfo getRequestInfo() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new HttpRequestInfo("", "", "", "");
        }
        jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return new HttpRequestInfo(
                request.getRequestURI(),
                request.getMethod(),
                ip,
                request.getHeader("User-Agent")
        );
    }

    private String readAnnotationValue(Annotation[] annotations, String annotationClassName, String methodName) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                try {
                    Object value = annotation.annotationType().getMethod(methodName).invoke(annotation);
                    return value == null ? "" : String.valueOf(value);
                } catch (Exception ignore) {
                    return "";
                }
            }
        }
        return "";
    }

    private record HttpRequestInfo(String uri, String requestMethod, String ip, String userAgent) {
    }

    private record UserContext(String tenantId, String appId, String userId, String loginname, String username) {
    }
}
