package com.budwk.sp.device.gateway.support;

import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveResultDTO;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.starter.common.exception.BaseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class GatewayProtocolIdentityService {
    private final ObjectMapper objectMapper;
    private final long scriptTimeoutMs;
    private final ThreadPoolExecutor executorService;
    private final ScheduledExecutorService timeoutScheduler;

    public GatewayProtocolIdentityService(ObjectMapper objectMapper,
                                          @Value("${wk.device.gateway.script-timeout-ms:1500}") long scriptTimeoutMs) {
        this.objectMapper = objectMapper;
        this.scriptTimeoutMs = Math.max(scriptTimeoutMs, 200L);
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        this.executorService = new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "gateway-script-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "gateway-script-timeout");
            thread.setDaemon(true);
            return thread;
        });
    }

    public DeviceProtocolIdentityResolveResultDTO resolveIdentity(Device_protocol protocol, Map<String, Object> input) {
        if (protocol == null || Strings.isBlank(protocol.getScriptContent())) {
            return null;
        }
        ContextHolder holder = new ContextHolder();
        try {
            Future<DeviceProtocolIdentityResolveResultDTO> future = executorService.submit(() -> {
                try {
                    return runResolveIdentity(protocol.getScriptContent(), input, holder);
                } finally {
                    holder.close();
                }
            });
            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                if (!future.isDone()) {
                    holder.interrupt();
                }
            }, scriptTimeoutMs, TimeUnit.MILLISECONDS);
            try {
                DeviceProtocolIdentityResolveResultDTO result = future.get(scriptTimeoutMs + 100L, TimeUnit.MILLISECONDS);
                timeoutTask.cancel(false);
                return result;
            } catch (TimeoutException e) {
                timeoutTask.cancel(false);
                holder.interrupt();
                future.cancel(true);
                throw new BaseException("身份预解析脚本执行超时 (" + scriptTimeoutMs + "ms)");
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new BaseException(Strings.sBlank(cause.getMessage(), "身份预解析脚本执行失败"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("身份预解析脚本执行被中断");
        } catch (RejectedExecutionException e) {
            throw new BaseException("脚本执行队列已满，请稍后重试");
        }
    }

    private DeviceProtocolIdentityResolveResultDTO runResolveIdentity(String scriptContent,
                                                                      Map<String, Object> input,
                                                                      ContextHolder holder) throws Exception {
        Context context = createSecureContext();
        holder.setContext(context);
        try {
            context.getBindings("js").putMember("inputJson",
                    objectMapper.writeValueAsString(prepareScriptInput(input)));
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

    private Map<String, Object> prepareScriptInput(Map<String, Object> input) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (input != null) {
            map.putAll(input);
        }
        map.put("direction", Strings.sBlank(readString(map.get("direction")), "U"));
        String hex = readPayloadHex(map);
        map.put("payload", resolvePayloadBytes(hex));
        map.put("payloadText", hex);
        return map;
    }

    private String readPayloadHex(Map<String, Object> input) {
        String hex = "";
        if (input != null) {
            Object payload = input.get("payload");
            if (payload != null && !(payload instanceof List<?>)) {
                hex = String.valueOf(payload);
            }
            if (Strings.isBlank(hex) && input.get("payloadText") != null) {
                hex = String.valueOf(input.get("payloadText"));
            }
        }
        return normalizeHex(hex);
    }

    private List<Integer> resolvePayloadBytes(String hex) {
        String normalized = normalizeHex(hex);
        List<Integer> bytes = new ArrayList<>();
        for (int i = 0; i < normalized.length(); i += 2) {
            bytes.add(Integer.parseInt(normalized.substring(i, i + 2), 16));
        }
        return bytes;
    }

    private String normalizeHex(String hex) {
        String normalized = Strings.sNull(hex).replaceAll("\\s+", "").trim();
        if (Strings.isBlank(normalized) || normalized.length() % 2 != 0) {
            throw new BaseException("payload 必须是偶数长度的16进制字符串");
        }
        return normalized.toUpperCase(Locale.ROOT);
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

    private String readString(Object value) {
        return value == null ? "" : Strings.sNull(String.valueOf(value)).trim();
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
        timeoutScheduler.shutdownNow();
    }

    private static class ContextHolder {
        private volatile Context context;

        public void setContext(Context context) {
            this.context = context;
        }

        public void interrupt() {
            if (context != null) {
                try {
                    context.interrupt(Duration.ZERO);
                } catch (Exception ignored) {
                }
            }
        }

        public void close() {
            if (context != null) {
                try {
                    context.close(true);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
