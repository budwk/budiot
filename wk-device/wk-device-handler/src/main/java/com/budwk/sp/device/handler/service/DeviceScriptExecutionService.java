package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolEncodeRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveResultDTO;
import com.budwk.sp.device.dto.DeviceParsedEventDTO;
import com.budwk.sp.device.dto.DeviceParsedPropertyDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;
import com.budwk.sp.device.dto.DeviceProtocolReplyDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.enums.DeviceMessageType;
import com.budwk.sp.device.enums.DeviceProtocolScriptType;
import com.budwk.sp.device.handler.config.DeviceHandlerProperties;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 设备协议脚本执行服务
 * 
 * 改进点：
 * 1. 使用 GraalVM Context.interrupt() 实现真正的超时中断
 * 2. 使用独立的有界线程池，防止资源耗尽
 * 3. 增加脚本执行统计和监控
 */
@Slf4j
@Service
public class DeviceScriptExecutionService {
    private final DeviceHandlerProperties properties;
    private final ObjectMapper objectMapper;
    
    // 使用有界线程池，防止恶意脚本耗尽资源
    private final ThreadPoolExecutor executorService;
    
    // 用于超时控制的调度器
    private final ScheduledExecutorService timeoutScheduler;
    
    // 统计信息
    private final Map<String, ScriptStats> statsMap = new ConcurrentHashMap<>();

    public DeviceScriptExecutionService(DeviceHandlerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        
        // 创建有界线程池
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        int queueCapacity = 100;
        
        this.executorService = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "script-exec-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程执行
        );
        
        // 创建超时调度器
        this.timeoutScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "script-timeout-checker");
            thread.setDaemon(true);
            return thread;
        });
        
        log.info("脚本执行服务已初始化: 核心线程={}, 最大线程={}, 队列容量={}", 
            corePoolSize, maxPoolSize, queueCapacity);
    }

    public DeviceProtocolParseResultDTO execute(Device_protocol protocol, Map<String, Object> input) {
        if (protocol == null || protocol.getScriptType() != DeviceProtocolScriptType.JAVASCRIPT) {
            throw new BaseException("暂不支持该脚本类型");
        }
        if (Strings.isBlank(protocol.getScriptContent())) {
            throw new BaseException("协议脚本内容不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        String protocolId = protocol.getId();
        
        try {
            // 使用上下文持有器来管理 Context 生命周期
            ContextHolder contextHolder = new ContextHolder();
            
            Future<DeviceProtocolParseResultDTO> future = executorService.submit(() -> {
                try {
                    return runJavascriptWithContext(protocol.getScriptContent(), input, contextHolder);
                } finally {
                    contextHolder.close();
                }
            });
            
            // 设置超时时间
            long timeoutMs = Math.max(properties.getScriptTimeoutMs(), 200L);
            
            // 调度超时中断任务
            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                if (!future.isDone()) {
                    contextHolder.interrupt();
                    log.warn("脚本执行超时，已发送中断信号: protocolId={}, timeout={}ms", protocolId, timeoutMs);
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
            
            try {
                DeviceProtocolParseResultDTO result = future.get(timeoutMs + 100, TimeUnit.MILLISECONDS); // 额外100ms等待中断生效
                timeoutTask.cancel(false);
                
                // 记录成功统计
                recordSuccess(protocolId, System.currentTimeMillis() - startTime);
                
                if (result.getProperties() == null) result.setProperties(new ArrayList<>());
                if (result.getEvents() == null) result.setEvents(new ArrayList<>());
                if (result.getReplies() == null) result.setReplies(new ArrayList<>());
                return result;
                
            } catch (TimeoutException e) {
                timeoutTask.cancel(false);
                contextHolder.interrupt();
                future.cancel(true);
                
                // 记录超时统计
                recordTimeout(protocolId);
                
                throw new BaseException("协议脚本执行超时 (" + timeoutMs + "ms)");
            }
            
        } catch (ExecutionException e) {
            recordError(protocolId);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BaseException(Strings.sBlank(cause.getMessage(), "协议脚本执行失败"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordError(protocolId);
            throw new BaseException("协议脚本执行被中断");
        } catch (RejectedExecutionException e) {
            recordError(protocolId);
            throw new BaseException("脚本执行队列已满，请稍后重试");
        }
    }

    public DeviceProtocolParseResultDTO debug(DeviceProtocolDebugRequestDTO request, Map<String, Object> input) {
        if (request == null) {
            throw new BaseException("调试请求不能为空");
        }
        Device_protocol protocol = new Device_protocol();
        protocol.setScriptType(request.getScriptType());
        protocol.setScriptContent(request.getScriptContent());
        return execute(protocol, input);
    }

    public DeviceProtocolIdentityResolveResultDTO resolveIdentity(Device_protocol protocol, Map<String, Object> input) {
        if (protocol == null || protocol.getScriptType() != DeviceProtocolScriptType.JAVASCRIPT) {
            throw new BaseException("暂不支持该脚本类型");
        }
        if (Strings.isBlank(protocol.getScriptContent())) {
            throw new BaseException("协议脚本内容不能为空");
        }

        String protocolId = Strings.sBlank(protocol.getId(), "resolveIdentity");
        long startTime = System.currentTimeMillis();

        try {
            ContextHolder contextHolder = new ContextHolder();

            Future<DeviceProtocolIdentityResolveResultDTO> future = executorService.submit(() -> {
                try {
                    return runJavascriptResolveIdentityWithContext(protocol.getScriptContent(), input, contextHolder);
                } finally {
                    contextHolder.close();
                }
            });

            long timeoutMs = Math.max(properties.getScriptTimeoutMs(), 200L);
            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                if (!future.isDone()) {
                    contextHolder.interrupt();
                    log.warn("身份预解析脚本执行超时，已发送中断信号: protocolId={}, timeout={}ms", protocolId, timeoutMs);
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);

            try {
                DeviceProtocolIdentityResolveResultDTO result = future.get(timeoutMs + 100, TimeUnit.MILLISECONDS);
                timeoutTask.cancel(false);
                recordSuccess(protocolId + "#resolveIdentity", System.currentTimeMillis() - startTime);
                return result;
            } catch (TimeoutException e) {
                timeoutTask.cancel(false);
                contextHolder.interrupt();
                future.cancel(true);
                recordTimeout(protocolId + "#resolveIdentity");
                throw new BaseException("身份预解析脚本执行超时 (" + timeoutMs + "ms)");
            }
        } catch (ExecutionException e) {
            recordError(protocolId + "#resolveIdentity");
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BaseException(Strings.sBlank(cause.getMessage(), "身份预解析脚本执行失败"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordError(protocolId + "#resolveIdentity");
            throw new BaseException("身份预解析脚本执行被中断");
        } catch (RejectedExecutionException e) {
            recordError(protocolId + "#resolveIdentity");
            throw new BaseException("脚本执行队列已满，请稍后重试");
        }
    }

    public String encode(Device_protocol protocol, String commandCode, String payloadJson) {
        return encode(protocol, commandCode, payloadJson, null);
    }

    public String encode(Device_protocol protocol, String commandCode, String payloadJson, Device_info device) {
        if (protocol == null || protocol.getScriptType() != DeviceProtocolScriptType.JAVASCRIPT) {
            throw new BaseException("暂不支持该脚本类型");
        }
        DeviceProtocolEncodeRequestDTO request = new DeviceProtocolEncodeRequestDTO();
        request.setScriptType(protocol.getScriptType());
        request.setScriptContent(protocol.getScriptContent());
        request.setCommandJson(buildCommandJson(commandCode, payloadJson, device));
        return encode(request);
    }

    public String encode(DeviceProtocolEncodeRequestDTO request) {
        if (request == null || request.getScriptType() != DeviceProtocolScriptType.JAVASCRIPT) {
            throw new BaseException("暂不支持该脚本类型");
        }
        if (Strings.isBlank(request.getScriptContent())) {
            throw new BaseException("协议脚本内容不能为空");
        }
        
        String protocolId = "encode";
        long startTime = System.currentTimeMillis();
        
        try {
            ContextHolder contextHolder = new ContextHolder();
            
            Future<String> future = executorService.submit(() -> {
                try {
                    return runJavascriptEncodeWithContext(request.getScriptContent(), request.getCommandJson(), contextHolder);
                } finally {
                    contextHolder.close();
                }
            });
            
            long timeoutMs = Math.max(properties.getScriptTimeoutMs(), 200L);
            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                if (!future.isDone()) {
                    contextHolder.interrupt();
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
            
            try {
                String result = future.get(timeoutMs + 100, TimeUnit.MILLISECONDS);
                timeoutTask.cancel(false);
                
                if (Strings.isBlank(result)) {
                    throw new BaseException("协议脚本编码结果不能为空");
                }
                
                recordSuccess(protocolId, System.currentTimeMillis() - startTime);
                return result;
                
            } catch (TimeoutException e) {
                timeoutTask.cancel(false);
                contextHolder.interrupt();
                future.cancel(true);
                recordTimeout(protocolId);
                throw new BaseException("协议脚本执行超时");
            }
            
        } catch (ExecutionException e) {
            recordError(protocolId);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BaseException(Strings.sBlank(cause.getMessage(), "协议脚本编码失败"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordError(protocolId);
            throw new BaseException("协议脚本执行被中断");
        } catch (RejectedExecutionException e) {
            recordError(protocolId);
            throw new BaseException("脚本执行队列已满，请稍后重试");
        }
    }

    /**
     * 使用 Context 持有器执行 JavaScript 脚本
     */
    private DeviceProtocolParseResultDTO runJavascriptWithContext(
            String scriptContent, 
            Map<String, Object> input,
            ContextHolder holder) throws Exception {
        
        if (!useRunEntry(scriptContent)) {
            return runLegacyJavascriptWithContext(scriptContent, input, holder);
        }
        
        Context context = createSecureContext();
        holder.setContext(context);
        
        try {
            context.getBindings("js").putMember("inputJson", 
                objectMapper.writeValueAsString(prepareScriptInput(input, "U")));
            
            context.eval("js", """
                const __wkInputSource = JSON.parse(inputJson);
                const input = {
                  get: function(key) {
                    return __wkInputSource[key];
                  },
                  has: function(key) {
                    return Object.prototype.hasOwnProperty.call(__wkInputSource, key);
                  }
                };
                """);
            
            context.eval("js", normalizeRunScript(scriptContent));
            
            // 检查中断状态
            if (Thread.currentThread().isInterrupted()) {
                throw new BaseException("脚本执行被中断");
            }
            
            String resultJson = context.eval("js", "JSON.stringify(run(input))").asString();
            
            if (Strings.isBlank(resultJson) || "null".equals(resultJson) || "undefined".equals(resultJson)) {
                throw new BaseException("协议脚本返回结果不能为空");
            }
            
            Map<String, Object> resultMap = objectMapper.readValue(resultJson, new TypeReference<>() {});
            return normalizeDecodeResult(resultMap);
            
        } finally {
            context.close(true);
        }
    }

    private String runJavascriptEncodeWithContext(
            String scriptContent, 
            String commandJson,
            ContextHolder holder) throws Exception {
        
        if (!useRunEntry(scriptContent)) {
            return runLegacyJavascriptEncodeWithContext(scriptContent, commandJson, holder);
        }
        
        Context context = createSecureContext();
        holder.setContext(context);
        
        try {
            Map<String, Object> command = objectMapper.readValue(commandJson, new TypeReference<>() {});
            context.getBindings("js").putMember("inputJson", 
                objectMapper.writeValueAsString(prepareScriptInput(command, "D")));
            
            context.eval("js", """
                const __wkInputSource = JSON.parse(inputJson);
                const input = {
                  get: function(key) {
                    return __wkInputSource[key];
                  },
                  has: function(key) {
                    return Object.prototype.hasOwnProperty.call(__wkInputSource, key);
                  }
                };
                """);
            
            context.eval("js", normalizeRunScript(scriptContent));
            
            String resultJson = context.eval("js", "JSON.stringify(run(input))").asString();
            String encoded = extractReplyPayload(objectMapper.readValue(resultJson, Object.class));
            
            if (Strings.isBlank(encoded)) {
                throw new BaseException("协议脚本编码结果不能为空");
            }
            return encoded;
            
        } finally {
            context.close(true);
        }
    }

    private DeviceProtocolIdentityResolveResultDTO runJavascriptResolveIdentityWithContext(
            String scriptContent,
            Map<String, Object> input,
            ContextHolder holder) throws Exception {

        Context context = createSecureContext();
        holder.setContext(context);

        try {
            context.getBindings("js").putMember("inputJson",
                    objectMapper.writeValueAsString(prepareScriptInput(input, "U")));

            context.eval("js", """
                const __wkInputSource = JSON.parse(inputJson);
                const input = {
                  get: function(key) {
                    return __wkInputSource[key];
                  },
                  has: function(key) {
                    return Object.prototype.hasOwnProperty.call(__wkInputSource, key);
                  }
                };
                """);

            context.eval("js", normalizeRunScript(scriptContent));

            String resultJson = context.eval("js", """
                (() => {
                  if (typeof resolveIdentity === 'function') {
                    return JSON.stringify(resolveIdentity(input));
                  }
                  return "";
                })()
                """).asString();

            if (Strings.isBlank(resultJson) || "null".equals(resultJson) || "undefined".equals(resultJson)) {
                return null;
            }

            Map<String, Object> resultMap = objectMapper.readValue(resultJson, new TypeReference<>() {});
            return normalizeIdentityResolveResult(resultMap);

        } finally {
            context.close(true);
        }
    }

    private DeviceProtocolParseResultDTO runLegacyJavascriptWithContext(
            String scriptContent, 
            Map<String, Object> input,
            ContextHolder holder) throws Exception {
        
        String payloadBytesJson = objectMapper.writeValueAsString(resolvePayloadBytes(input));
        Context context = createSecureContext();
        holder.setContext(context);
        
        try {
            context.getBindings("js").putMember("payloadBytesJson", payloadBytesJson);
            context.eval("js", normalizeScriptContent(scriptContent));
            String resultJson = context.eval("js", "JSON.stringify(decode(JSON.parse(payloadBytesJson)))").asString();
            
            if (Strings.isBlank(resultJson) || "null".equals(resultJson) || "undefined".equals(resultJson)) {
                throw new BaseException("协议脚本返回结果不能为空");
            }
            
            Map<String, Object> resultMap = objectMapper.readValue(resultJson, new TypeReference<>() {});
            return normalizeDecodeResult(resultMap);
            
        } finally {
            context.close(true);
        }
    }

    private String runLegacyJavascriptEncodeWithContext(
            String scriptContent, 
            String commandJson,
            ContextHolder holder) throws Exception {
        
        Context context = createSecureContext();
        holder.setContext(context);
        
        try {
            context.getBindings("js").putMember("commandJson", commandJson);
            context.eval("js", normalizeScriptContent(scriptContent));
            
            String encoded = context.eval("js", """
                (function () {
                  const result = encode(JSON.parse(commandJson));
                  if (result === null || result === undefined) {
                    return '';
                  }
                  if (typeof result === 'string') {
                    return result;
                  }
                  return JSON.stringify(Array.from(result));
                })()
                """).asString();
            
            if (Strings.isBlank(encoded)) {
                throw new BaseException("协议脚本编码结果不能为空");
            }
            
            String trimmed = encoded.trim();
            if (trimmed.startsWith("[")) {
                List<Integer> bytes = objectMapper.readValue(trimmed, new TypeReference<>() {});
                return bytesToHex(bytes);
            }
            return trimmed;
            
        } finally {
            context.close(true);
        }
    }

    /**
     * 创建安全的 GraalVM Context
     */
    private Context createSecureContext() {
        return Context.newBuilder("js")
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowIO(false)
                .allowNativeAccess(false)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    // ==================== 统计记录方法 ====================
    
    private void recordSuccess(String protocolId, long durationMs) {
        statsMap.computeIfAbsent(protocolId, k -> new ScriptStats())
                .recordSuccess(durationMs);
    }
    
    private void recordTimeout(String protocolId) {
        statsMap.computeIfAbsent(protocolId, k -> new ScriptStats())
                .recordTimeout();
    }
    
    private void recordError(String protocolId) {
        statsMap.computeIfAbsent(protocolId, k -> new ScriptStats())
                .recordError();
    }
    
    /**
     * 获取脚本执行统计信息
     */
    public Map<String, Object> getStats(String protocolId) {
        ScriptStats stats = statsMap.get(protocolId);
        if (stats == null) {
            return Map.of(
                "totalExecutions", 0,
                "successCount", 0,
                "timeoutCount", 0,
                "errorCount", 0,
                "avgDurationMs", 0
            );
        }
        return stats.toMap();
    }
    
    /**
     * 获取线程池状态
     */
    public Map<String, Object> getPoolStats() {
        return Map.of(
            "activeThreads", executorService.getActiveCount(),
            "poolSize", executorService.getPoolSize(),
            "queueSize", executorService.getQueue().size(),
            "completedTasks", executorService.getCompletedTaskCount()
        );
    }

    // ==================== 以下为原有的辅助方法，保持不变 ====================

    private DeviceProtocolParseResultDTO normalizeDecodeResult(Map<String, Object> decoded) throws Exception {
        if (decoded == null || decoded.isEmpty()) {
            throw new BaseException("协议脚本返回结果不能为空");
        }
        String error = readString(decoded.get("error"));
        if (Strings.isNotBlank(error)) {
            throw new BaseException(error);
        }
        if (decoded.containsKey("properties") || decoded.containsKey("events") || decoded.containsKey("replies")) {
            return normalizeStructuredResult(decoded);
        }
        DeviceProtocolParseResultDTO result = new DeviceProtocolParseResultDTO();
        result.setMessageType(Strings.sBlank(readString(decoded.get("type")), DeviceMessageType.RAW.getValue()));
        result.setDeviceAt(readLong(decoded.get("timestamp")));
        result.setMetadataJson(objectMapper.writeValueAsString(decoded));
        appendDirectValueProperty(result, decoded);
        appendValueProperties(result, decoded);
        appendFlatProperties(result, decoded);
        if (result.getProperties().isEmpty() && result.getEvents().isEmpty() && result.getReplies().isEmpty()) {
            DeviceParsedPropertyDTO property = new DeviceParsedPropertyDTO();
            property.setIdentifier(toIdentifier(Strings.sBlank(result.getMessageType(), "result")));
            property.setName(Strings.sBlank(result.getMessageType(), "result"));
            property.setValueJson(result.getMetadataJson());
            property.setUnit("");
            result.getProperties().add(property);
        }
        return result;
    }

    private DeviceProtocolIdentityResolveResultDTO normalizeIdentityResolveResult(Map<String, Object> resolved) {
        if (resolved == null || resolved.isEmpty()) {
            return null;
        }
        DeviceProtocolIdentityResolveResultDTO result = new DeviceProtocolIdentityResolveResultDTO();
        result.setIdentityType(readString(resolved.get("identityType")));
        result.setIdentityValue(readString(resolved.get("identityValue")));
        result.setProductKey(readString(resolved.get("productKey")));
        result.setDeviceCode(readString(resolved.get("deviceCode")));
        result.setImei(readString(resolved.get("imei")));
        result.setIccid(readString(resolved.get("iccid")));
        if (Strings.isBlank(result.getIdentityValue())) {
            if (Strings.isNotBlank(result.getDeviceCode())) {
                result.setIdentityType("DEVICE_CODE");
                result.setIdentityValue(result.getDeviceCode());
            } else if (Strings.isNotBlank(result.getImei())) {
                result.setIdentityType("IMEI");
                result.setIdentityValue(result.getImei());
            } else if (Strings.isNotBlank(result.getIccid())) {
                result.setIdentityType("ICCID");
                result.setIdentityValue(result.getIccid());
            }
        }
        if (Strings.isBlank(result.getIdentityType()) && Strings.isNotBlank(result.getIdentityValue())) {
            result.setIdentityType("DEVICE_CODE");
        }
        return Strings.isBlank(result.getIdentityValue()) ? null : result;
    }

    private DeviceProtocolParseResultDTO normalizeStructuredResult(Map<String, Object> decoded) throws Exception {
        DeviceProtocolParseResultDTO result = new DeviceProtocolParseResultDTO();
        result.setMetadataJson(objectMapper.writeValueAsString(decoded));
        result.setDeviceAt(firstNonNull(readLong(decoded.get("deviceAt")), readLong(decoded.get("time")), readLong(decoded.get("timestamp"))));
        appendStructuredProperties(result, decoded.get("properties"));
        appendStructuredEvents(result, decoded.get("events"));
        appendStructuredReplies(result, decoded.get("replies"));
        result.setMessageType(resolveStructuredMessageType(decoded, result));
        return result;
    }

    private void appendDirectValueProperty(DeviceProtocolParseResultDTO result, Map<String, Object> decoded) throws Exception {
        if (!decoded.containsKey("value")) {
            return;
        }
        DeviceParsedPropertyDTO property = new DeviceParsedPropertyDTO();
        String identifier = Strings.sBlank(readString(decoded.get("identifier")),
                Strings.sBlank(readString(decoded.get("type")),
                        Strings.sBlank(readString(decoded.get("cmd")), "value")));
        String name = Strings.sBlank(readString(decoded.get("name")),
                Strings.sBlank(readString(decoded.get("type")),
                        Strings.sBlank(readString(decoded.get("cmd")), identifier)));
        property.setIdentifier(toIdentifier(identifier));
        property.setName(name);
        property.setValueJson(objectMapper.writeValueAsString(decoded.get("value")));
        property.setUnit(readString(decoded.get("unit")));
        property.setDeviceAt(firstNonNull(readLong(decoded.get("deviceAt")), readLong(decoded.get("time")), readLong(decoded.get("timestamp"))));
        result.getProperties().add(property);
    }

    private void appendStructuredProperties(DeviceProtocolParseResultDTO result, Object properties) throws Exception {
        if (!(properties instanceof Collection<?> items)) {
            return;
        }
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            Map<String, Object> map = item instanceof Map<?, ?> source ? castMap(source) : new LinkedHashMap<>();
            DeviceParsedPropertyDTO property = new DeviceParsedPropertyDTO();
            String identifier = Strings.sBlank(readString(map.get("identifier")), Strings.sBlank(readString(map.get("name")), "property"));
            property.setIdentifier(toIdentifier(identifier));
            property.setName(Strings.sBlank(readString(map.get("name")), identifier));
            if (map.containsKey("value")) {
                property.setValueJson(objectMapper.writeValueAsString(map.get("value")));
            } else if (Strings.isNotBlank(readString(map.get("valueJson")))) {
                property.setValueJson(readString(map.get("valueJson")));
            } else {
                property.setValueJson(objectMapper.writeValueAsString(item));
            }
            property.setUnit(readString(map.get("unit")));
            property.setDeviceAt(firstNonNull(readLong(map.get("deviceAt")), readLong(map.get("time")), readLong(map.get("timestamp"))));
            result.getProperties().add(property);
        }
    }

    private void appendStructuredEvents(DeviceProtocolParseResultDTO result, Object events) throws Exception {
        if (!(events instanceof Collection<?> items)) {
            return;
        }
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            Map<String, Object> map = item instanceof Map<?, ?> source ? castMap(source) : new LinkedHashMap<>();
            DeviceParsedEventDTO event = new DeviceParsedEventDTO();
            String eventCode = Strings.sBlank(readString(map.get("eventCode")), Strings.sBlank(readString(map.get("identifier")), "event"));
            event.setEventCode(eventCode);
            event.setEventName(Strings.sBlank(readString(map.get("eventName")), Strings.sBlank(readString(map.get("msg")), eventCode)));
            event.setLevel(readString(map.get("level")));
            event.setSourceType(readString(map.get("sourceType")));
            event.setDeviceAt(firstNonNull(readLong(map.get("deviceAt")), readLong(map.get("time")), readLong(map.get("timestamp"))));
            event.setContentJson(buildEventContentJson(item, map));
            result.getEvents().add(event);
        }
    }

    private void appendStructuredReplies(DeviceProtocolParseResultDTO result, Object replies) {
        if (!(replies instanceof Collection<?> items)) {
            return;
        }
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            DeviceProtocolReplyDTO reply = new DeviceProtocolReplyDTO();
            if (item instanceof Map<?, ?> source) {
                Map<String, Object> map = castMap(source);
                reply.setCommandCode(Strings.sBlank(readString(map.get("commandCode")), Strings.sBlank(readString(map.get("method")), readString(map.get("cmd")))));
                reply.setPayload(extractReplyPayload(map));
                reply.setPayloadJson(readString(map.get("payloadJson")));
                reply.setReplyRequired(readBoolean(map.get("replyRequired")));
                reply.setDeadlineAt(firstNonNull(readLong(map.get("deadlineAt")), readLong(map.get("expireAt"))));
            } else {
                reply.setPayload(extractReplyPayload(item));
            }
            if (Strings.isBlank(reply.getPayload()) && Strings.isBlank(reply.getPayloadJson())) {
                continue;
            }
            result.getReplies().add(reply);
        }
    }

    private void appendValueProperties(DeviceProtocolParseResultDTO result, Map<String, Object> decoded) throws Exception {
        Object values = decoded.get("values");
        if (!(values instanceof Map<?, ?> valuesMap)) {
            return;
        }
        String commonUnit = valuesMap.get("unit") == null ? "" : readString(valuesMap.get("unit"));
        for (Map.Entry<?, ?> entry : valuesMap.entrySet()) {
            String key = readString(entry.getKey());
            if (Strings.isBlank(key) || "unit".equalsIgnoreCase(key)) {
                continue;
            }
            DeviceParsedPropertyDTO property = new DeviceParsedPropertyDTO();
            property.setIdentifier(toIdentifier(key));
            property.setName(key);
            if (entry.getValue() instanceof Map<?, ?> nested) {
                Object nestedValue = nested.containsKey("value") ? nested.get("value") : nested;
                Object nestedUnit = nested.containsKey("unit") ? nested.get("unit") : commonUnit;
                property.setValueJson(objectMapper.writeValueAsString(nestedValue));
                property.setUnit(readString(nestedUnit));
                property.setName(Strings.sBlank(readString(nested.get("name")), key));
            } else {
                property.setValueJson(objectMapper.writeValueAsString(entry.getValue()));
                property.setUnit(commonUnit);
            }
            property.setDeviceAt(firstNonNull(readLong(decoded.get("deviceAt")), readLong(decoded.get("time")), readLong(decoded.get("timestamp"))));
            result.getProperties().add(property);
        }
    }

    private void appendFlatProperties(DeviceProtocolParseResultDTO result, Map<String, Object> decoded) throws Exception {
        Set<String> skip = Set.of("cmd", "type", "timestamp", "raw", "error", "values", "value", "unit",
                "address", "command", "success", "identifier", "name",
                "properties", "events", "replies", "metadataJson", "messageType", "deviceAt");
        for (Map.Entry<String, Object> entry : decoded.entrySet()) {
            if (skip.contains(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            if (entry.getValue() instanceof Map<?, ?> || entry.getValue() instanceof Collection<?>) {
                continue;
            }
            DeviceParsedPropertyDTO property = new DeviceParsedPropertyDTO();
            property.setIdentifier(toIdentifier(entry.getKey()));
            property.setName(entry.getKey());
            property.setValueJson(objectMapper.writeValueAsString(entry.getValue()));
            property.setUnit("");
            result.getProperties().add(property);
        }
    }

    private String buildCommandJson(String commandCode, String payloadJson, Device_info device) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("direction", "D");
        map.put("cmd", Strings.sNull(commandCode).trim());
        map.put("method", Strings.sNull(commandCode).trim());
        if (device != null) {
            map.put("deviceId", Strings.sBlank(device.getId(), ""));
            map.put("deviceCode", Strings.sBlank(device.getDeviceCode(), ""));
            map.put("address", Strings.sBlank(device.getDeviceCode(), ""));
            map.put("imei", Strings.sBlank(device.getImei(), ""));
            map.put("iccid", Strings.sBlank(device.getIccid(), ""));
            map.put("productId", Strings.sBlank(device.getProductId(), ""));
            map.put("productKey", Strings.sBlank(device.getProductKey(), ""));
        }
        if (Strings.isNotBlank(payloadJson)) {
            try {
                Object payload = objectMapper.readValue(payloadJson, Object.class);
                if (payload instanceof Map<?, ?> payloadMap) {
                    payloadMap.forEach((key, value) -> map.put(String.valueOf(key), value));
                } else {
                    map.put("payload", payload);
                }
            } catch (Exception e) {
                map.put("payload", payloadJson);
            }
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new BaseException("指令JSON构建失败");
        }
    }

    private Map<String, Object> prepareScriptInput(Map<String, Object> input, String defaultDirection) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (input != null) {
            map.putAll(input);
        }
        String direction = Strings.sBlank(readString(map.get("direction")), defaultDirection);
        map.put("direction", direction);
        if ("U".equalsIgnoreCase(direction)) {
            String hex = readPayloadHex(map);
            map.put("payload", resolvePayloadBytes(hex));
            map.put("payloadText", hex);
        }
        return map;
    }

    private List<Integer> resolvePayloadBytes(Map<String, Object> input) {
        return resolvePayloadBytes(readPayloadHex(input));
    }

    private List<Integer> resolvePayloadBytes(String hex) {
        String normalized = normalizeHex(hex);
        List<Integer> bytes = new ArrayList<>();
        for (int i = 0; i < normalized.length(); i += 2) {
            bytes.add(Integer.parseInt(normalized.substring(i, i + 2), 16));
        }
        return bytes;
    }

    private String readPayloadHex(Map<String, Object> input) {
        String hex = "";
        if (input != null) {
            Object payload = input.get("payload");
            if (payload != null && !(payload instanceof Collection<?>)) {
                hex = String.valueOf(payload);
            }
            if (Strings.isBlank(hex) && input.get("payloadText") != null) {
                hex = String.valueOf(input.get("payloadText"));
            }
        }
        return normalizeHex(hex);
    }

    private String normalizeHex(String hex) {
        String normalized = Strings.sNull(hex).replaceAll("\\s+", "").trim();
        if (Strings.isBlank(normalized) || normalized.length() % 2 != 0) {
            throw new BaseException("payload 必须是偶数长度的16进制字符串");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private boolean useRunEntry(String scriptContent) {
        String normalized = normalizeScriptContent(scriptContent);
        return Strings.isNotBlank(normalized) && normalized.contains("run(");
    }

    private String normalizeRunScript(String scriptContent) {
        return normalizeScriptContent(scriptContent).replaceFirst("(?s)\\s*run\\(input\\);?\\s*$", "");
    }

    private String normalizeScriptContent(String scriptContent) {
        return Strings.sNull(scriptContent)
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private String extractReplyPayload(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text.trim();
        }
        if (value instanceof Collection<?> collection) {
            List<Integer> bytes = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof Number number) {
                    bytes.add(number.intValue());
                }
            }
            return bytes.isEmpty() ? "" : bytesToHex(bytes);
        }
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> map = castMap(source);
            String payload = readString(map.get("payload"));
            if (Strings.isNotBlank(payload)) {
                return payload;
            }
            return extractReplyPayload(map.get("replies") instanceof Collection<?> replies && !replies.isEmpty() ? replies.iterator().next() : null);
        }
        return readString(value);
    }

    private String resolveStructuredMessageType(Map<String, Object> decoded, DeviceProtocolParseResultDTO result) {
        String messageType = readString(decoded.get("type"));
        if (Strings.isNotBlank(messageType)) {
            return messageType;
        }
        if (result.getProperties() != null && !result.getProperties().isEmpty()) {
            return DeviceMessageType.PROPERTY.getValue();
        }
        if (result.getEvents() != null && !result.getEvents().isEmpty()) {
            return DeviceMessageType.EVENT.getValue();
        }
        return DeviceMessageType.RAW.getValue();
    }

    private String buildEventContentJson(Object item, Map<String, Object> map) throws Exception {
        if (Strings.isNotBlank(readString(map.get("contentJson")))) {
            return readString(map.get("contentJson"));
        }
        Map<String, Object> content = new LinkedHashMap<>();
        if (map.containsKey("value")) {
            content.put("value", map.get("value"));
        }
        if (Strings.isNotBlank(readString(map.get("msg")))) {
            content.put("msg", readString(map.get("msg")));
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (Set.of("identifier", "eventCode", "eventName", "name", "msg", "value", "time", "timestamp", "deviceAt", "level", "sourceType", "contentJson").contains(entry.getKey())) {
                continue;
            }
            content.put(entry.getKey(), entry.getValue());
        }
        return objectMapper.writeValueAsString(content.isEmpty() ? item : content);
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> map = new LinkedHashMap<>();
        source.forEach((key, value) -> map.put(String.valueOf(key), value));
        return map;
    }

    private String bytesToHex(List<Integer> bytes) {
        StringBuilder builder = new StringBuilder();
        for (Integer value : bytes) {
            int item = value == null ? 0 : value & 0xFF;
            builder.append(String.format("%02X", item));
        }
        return builder.toString();
    }

    private String toIdentifier(String value) {
        return Strings.sBlank(value, "result").trim().replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("[^a-zA-Z0-9_]+", "_").toLowerCase(Locale.ROOT);
    }

    private String readString(Object value) {
        return value == null ? "" : Strings.sNull(String.valueOf(value)).trim();
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = readString(value);
        return Strings.isBlank(text) ? null : Long.parseLong(text);
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = readString(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭脚本执行服务...");
        executorService.shutdownNow();
        timeoutScheduler.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("线程池未能在5秒内完全关闭");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 内部类 ====================

    /**
     * Context 持有器，用于管理 GraalVM Context 的生命周期和中断
     */
    private static class ContextHolder {
        private volatile Context context;
        private volatile boolean interrupted = false;

        public void setContext(Context context) {
            this.context = context;
        }

        public void interrupt() {
            this.interrupted = true;
            if (context != null) {
                try {
                    // 使用 GraalVM 的中断机制
                    context.interrupt(Duration.ZERO);
                } catch (Exception e) {
                    // 忽略中断异常
                }
            }
        }

        public void close() {
            if (context != null) {
                try {
                    context.close(true);
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 脚本执行统计信息
     */
    private static class ScriptStats {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong timeoutCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalDurationMs = new AtomicLong(0);

        public void recordSuccess(long durationMs) {
            totalExecutions.incrementAndGet();
            successCount.incrementAndGet();
            totalDurationMs.addAndGet(durationMs);
        }

        public void recordTimeout() {
            totalExecutions.incrementAndGet();
            timeoutCount.incrementAndGet();
        }

        public void recordError() {
            totalExecutions.incrementAndGet();
            errorCount.incrementAndGet();
        }

        public Map<String, Object> toMap() {
            long total = totalExecutions.get();
            long avgDuration = total > 0 ? totalDurationMs.get() / total : 0;
            return Map.of(
                "totalExecutions", total,
                "successCount", successCount.get(),
                "timeoutCount", timeoutCount.get(),
                "errorCount", errorCount.get(),
                "avgDurationMs", avgDuration
            );
        }
    }
}
