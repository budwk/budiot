package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.gateway.config.DeviceGatewayProperties;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.network.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GatewayNetworkManager {
    private final DeviceGatewayProperties properties;
    private final GatewayInboundService gatewayInboundService;
    private final GatewayNodeRegistryService gatewayNodeRegistryService;
    private final GatewaySessionService gatewaySessionService;
    private final GatewayAlertService alertService;
    private final Dao dao;
    private final StringRedisTemplate stringRedisTemplate;
    private final List<DeviceNetworkProvider> providers;
    private final Map<String, DeviceNetworkServer> serverMap = new ConcurrentHashMap<>();
    private final Map<String, DeviceGatewayBinding> bindingMap = new ConcurrentHashMap<>();

    public GatewayNetworkManager(DeviceGatewayProperties properties,
                                 GatewayInboundService gatewayInboundService,
                                 GatewayNodeRegistryService gatewayNodeRegistryService,
                                 GatewaySessionService gatewaySessionService,
                                 GatewayAlertService alertService,
                                 Dao dao,
                                 StringRedisTemplate stringRedisTemplate,
                                 List<DeviceNetworkProvider> providers) {
        this.properties = properties;
        this.gatewayInboundService = gatewayInboundService;
        this.gatewayNodeRegistryService = gatewayNodeRegistryService;
        this.gatewaySessionService = gatewaySessionService;
        this.alertService = alertService;
        this.dao = dao;
        this.stringRedisTemplate = stringRedisTemplate;
        this.providers = providers;
    }

    @PostConstruct
    public void start() throws Exception {
        sync();
    }

    @Scheduled(fixedDelayString = "${wk.device.gateway.sync-seconds:60}000")
    public synchronized void sync() throws Exception {
        Map<String, DeviceGatewayBinding> desired = loadBindings();
        for (Map.Entry<String, DeviceGatewayBinding> entry : desired.entrySet()) {
            DeviceGatewayBinding current = bindingMap.get(entry.getKey());
            if (current == null || !same(current, entry.getValue())) {
                stopServer(entry.getKey());
                startServer(entry.getValue());
            }
        }
        for (String gatewayId : new ArrayList<>(bindingMap.keySet())) {
            if (!desired.containsKey(gatewayId)) {
                stopServer(gatewayId);
            }
        }
        gatewaySessionService.registerLocalBindings(bindingMap.values());
        gatewayNodeRegistryService.syncBindings(bindingMap.values());
        refreshClaims();
    }

    /**
     * 定期检查网关健康状态，异常停止时自动重启
     */
    @Scheduled(fixedDelayString = "${wk.device.gateway.healthcheck-seconds:30}000")
    public synchronized void healthCheck() {
        for (Map.Entry<String, DeviceNetworkServer> entry : serverMap.entrySet()) {
            String gatewayId = entry.getKey();
            DeviceNetworkServer server = entry.getValue();
            
            if (!server.isRunning()) {
                log.warn("网关异常停止，尝试重启: gatewayId={}", gatewayId);
                alertService.alertAbnormalStop(gatewayId, gatewayId);
                
                DeviceGatewayBinding binding = bindingMap.get(gatewayId);
                
                if (binding != null) {
                    try {
                        // 停止异常网关
                        stopServer(gatewayId);
                        // 等待 1 秒后重启
                        Thread.sleep(1000);
                        // 尝试重启
                        startServer(binding);
                        
                        if (serverMap.containsKey(gatewayId) && serverMap.get(gatewayId).isRunning()) {
                            log.info("网关重启成功: gatewayId={}", gatewayId);
                        } else {
                            log.error("网关重启失败: gatewayId={}", gatewayId);
                            alertService.alertRestartFailed(gatewayId, gatewayId, "重启后仍然未运行");
                        }
                    } catch (Exception e) {
                        log.error("网关重启异常: gatewayId={}", gatewayId, e);
                        alertService.alertRestartFailed(gatewayId, gatewayId, e.getMessage());
                    }
                }
            }
        }
    }

    public synchronized void refreshGateway(String gatewayId) throws Exception {
        if (Strings.isBlank(gatewayId)) {
            return;
        }
        Device_gateway gateway = dao.fetch(Device_gateway.class, Cnd.where("id", "=", gatewayId).and("delFlag", "=", false));
        if (gateway == null || gateway.isDisabled() || gateway.getRuntimeStatus() != DeviceGatewayStatus.RUNNING) {
            stopServer(gatewayId);
            gatewaySessionService.registerLocalBindings(bindingMap.values());
            gatewayNodeRegistryService.syncBindings(bindingMap.values());
            return;
        }
        DeviceGatewayBinding binding = toBinding(gateway);
        DeviceGatewayBinding current = bindingMap.get(gatewayId);
        if (current == null || !same(current, binding)) {
            stopServer(gatewayId);
            startServer(binding);
        }
        gatewaySessionService.registerLocalBindings(bindingMap.values());
        gatewayNodeRegistryService.syncBindings(bindingMap.values());
        refreshClaims();
    }

    @PreDestroy
    public void stop() {
        gatewayNodeRegistryService.unregister(bindingMap.values());
        for (String gatewayId : new ArrayList<>(serverMap.keySet())) {
            stopServer(gatewayId);
        }
        serverMap.clear();
        bindingMap.clear();
    }

    public void send(DeviceNetworkDownlinkMessage message) {
        DeviceNetworkServer server = serverMap.get(message.getBindingId());
        if (server != null) server.send(message);
    }

    /**
     * 获取服务器映射（用于监控）
     */
    public Map<String, DeviceNetworkServer> getServerMap() {
        return new HashMap<>(serverMap);
    }

    /**
     * 获取绑定映射（用于监控）
     */
    public Map<String, DeviceGatewayBinding> getBindingMap() {
        return new HashMap<>(bindingMap);
    }

    private Map<String, DeviceGatewayBinding> loadBindings() {
        Map<String, DeviceGatewayBinding> map = new LinkedHashMap<>();
        List<Device_gateway> gateways = dao.query(Device_gateway.class, Cnd.where("delFlag", "=", false).and("disabled", "=", false).and("runtimeStatus", "=", DeviceGatewayStatus.RUNNING.name()));
        for (Device_gateway gateway : gateways) {
            DeviceGatewayBinding binding = toBinding(gateway);
            if (binding.getProtocol() != null && claimGateway(binding)) {
                map.put(gateway.getId(), binding);
            }
        }
        return map;
    }

    private DeviceGatewayBinding toBinding(Device_gateway gateway) {
        DeviceGatewayBinding binding = new DeviceGatewayBinding();
        binding.setGatewayId(gateway.getId());
        binding.setBindingId(gateway.getId());
        binding.setNodeId(gateway.getId());
        binding.setTenantId(gateway.getTenantId());
        binding.setProtocol(gateway.getNetworkProtocol());
        binding.setGatewayMode(gateway.getGatewayMode());
        binding.setProtocolId(gateway.getProtocolId());
        binding.setHost(Strings.sBlank(gateway.getHost(), properties.getHost()));
        binding.setPort(gateway.getPort() == null ? 0 : gateway.getPort());
        binding.setPath(Strings.sBlank(gateway.getPath(), "/"));
        binding.setRemoteHost(Strings.sNull(gateway.getRemoteHost()).trim());
        binding.setRemotePort(gateway.getRemotePort());
        binding.setClientId(Strings.sNull(gateway.getClientId()).trim());
        binding.setUsername(Strings.sNull(gateway.getUsername()).trim());
        binding.setPassword(Strings.sNull(gateway.getPassword()).trim());
        binding.setAllowAnonymous(gateway.isAllowAnonymous());
        binding.setSubscribeTopic(Strings.sNull(gateway.getSubscribeTopic()).trim());
        binding.setPublishTopic(Strings.sNull(gateway.getPublishTopic()).trim());
        binding.setDownlinkTopicPrefix(Strings.sBlank(gateway.getPublishTopic(), "cmd/"));
        binding.setEnabled(true);
        return binding;
    }

    private void startServer(DeviceGatewayBinding binding) {
        if (!claimGateway(binding)) {
            log.warn("网关认领失败，跳过启动: gatewayId={}, tenantId={}, host={}, port={}",
                    binding.getGatewayId(), binding.getTenantId(), binding.getHost(), binding.getPort());
            return;
        }
        DeviceNetworkProvider provider = providers.stream().filter(item -> item.supports(binding)).findFirst().orElse(null);
        if (provider == null) {
            updateRuntime(binding, DeviceGatewayStatus.ERROR, "未找到匹配的网络组件");
            alertService.alertStartFailed(binding.getGatewayId(), binding.getGatewayId(), "未找到匹配的网络组件");
            return;
        }
        try {
            log.info("开始启动网关: gatewayId={}, protocol={}, mode={}, host={}, port={}",
                    binding.getGatewayId(), binding.getProtocol(), binding.getGatewayMode(), binding.getHost(), binding.getPort());
            DeviceNetworkServer server = provider.create(binding, gatewayInboundService);
            server.start();
            serverMap.put(binding.getNodeId(), server);
            bindingMap.put(binding.getGatewayId(), binding);
            updateRuntime(binding, DeviceGatewayStatus.RUNNING, "");
            log.info("网关启动成功: gatewayId={}, host={}, port={}",
                    binding.getGatewayId(), binding.getHost(), binding.getPort());
        } catch (java.net.BindException e) {
            String error = "端口已被占用: " + binding.getHost() + ":" + binding.getPort();
            updateRuntime(binding, DeviceGatewayStatus.ERROR, error);
            alertService.alertPortConflict(binding.getGatewayId(), binding.getHost(), binding.getPort());
            log.error("网关启动失败，端口冲突: gatewayId={}, host={}, port={}",
                    binding.getGatewayId(), binding.getHost(), binding.getPort(), e);
            releaseClaim(binding);
        } catch (Exception e) {
            String error = Strings.sBlank(e.getMessage(), e.getClass().getSimpleName());
            updateRuntime(binding, DeviceGatewayStatus.ERROR, error);
            alertService.alertStartFailed(binding.getGatewayId(), binding.getGatewayId(), error);
            log.error("网关启动失败: gatewayId={}, host={}, port={}",
                    binding.getGatewayId(), binding.getHost(), binding.getPort(), e);
            releaseClaim(binding);
        }
    }

    private void stopServer(String gatewayId) {
        DeviceGatewayBinding binding = bindingMap.remove(gatewayId);
        DeviceNetworkServer server = serverMap.remove(gatewayId);
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ignored) {
            }
        }
        if (binding != null) {
            releaseClaim(binding);
            updateRuntime(binding, DeviceGatewayStatus.STOPPED, "");
        }
    }

    private void updateRuntime(DeviceGatewayBinding binding, DeviceGatewayStatus status, String error) {
        Device_gateway gateway = dao.fetch(Device_gateway.class, binding.getGatewayId());
        if (gateway == null) return;
        gateway.setRuntimeStatus(status);
        gateway.setLastError(Strings.sNull(error).trim());
        if (status == DeviceGatewayStatus.RUNNING) {
            gateway.setLastStartedAt(System.currentTimeMillis());
        }
        dao.updateIgnoreNull(gateway);
    }

    private boolean same(DeviceGatewayBinding a, DeviceGatewayBinding b) {
        return Objects.equals(a.getProtocol(), b.getProtocol())
                && Objects.equals(a.getGatewayMode(), b.getGatewayMode())
                && Objects.equals(a.getHost(), b.getHost())
                && a.getPort() == b.getPort()
                && Objects.equals(a.getPath(), b.getPath())
                && Objects.equals(a.getRemoteHost(), b.getRemoteHost())
                && Objects.equals(a.getRemotePort(), b.getRemotePort())
                && Objects.equals(a.getClientId(), b.getClientId())
                && Objects.equals(a.getUsername(), b.getUsername())
                && Objects.equals(a.getPassword(), b.getPassword())
                && Objects.equals(a.getSubscribeTopic(), b.getSubscribeTopic())
                && Objects.equals(a.getPublishTopic(), b.getPublishTopic())
                && Objects.equals(a.getProtocolId(), b.getProtocolId());
    }

    private boolean claimGateway(DeviceGatewayBinding binding) {
        String key = ownerKey(binding);
        String owner = stringRedisTemplate.opsForValue().get(key);
        if (Strings.isBlank(owner)) {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, properties.getNodeId(), Duration.ofSeconds(Math.max(properties.getHeartbeatSeconds() * 3L, 30L)));
            return Boolean.TRUE.equals(success);
        }
        if (properties.getNodeId().equals(owner)) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(Math.max(properties.getHeartbeatSeconds() * 3L, 30L)));
            return true;
        }
        if (!isGatewayOwnerAlive(owner)) {
            log.warn("检测到失活网关 owner，尝试接管: gatewayId={}, staleOwner={}, newOwner={}",
                    binding.getGatewayId(), owner, properties.getNodeId());
            stringRedisTemplate.delete(key);
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    properties.getNodeId(),
                    Duration.ofSeconds(Math.max(properties.getHeartbeatSeconds() * 3L, 30L))
            );
            return Boolean.TRUE.equals(success);
        }
        return false;
    }

    private void releaseClaim(DeviceGatewayBinding binding) {
        String key = ownerKey(binding);
        String owner = stringRedisTemplate.opsForValue().get(key);
        if (properties.getNodeId().equals(owner)) {
            stringRedisTemplate.delete(key);
        }
    }

    private void refreshClaims() {
        Duration ttl = Duration.ofSeconds(Math.max(properties.getHeartbeatSeconds() * 3L, 30L));
        for (DeviceGatewayBinding binding : bindingMap.values()) {
            String key = ownerKey(binding);
            String owner = stringRedisTemplate.opsForValue().get(key);
            if (properties.getNodeId().equals(owner)) {
                stringRedisTemplate.expire(key, ttl);
            }
        }
    }

    private String ownerKey(DeviceGatewayBinding binding) {
        return "wk:device:gateway:owner:" + binding.getTenantId() + ":" + binding.getGatewayId();
    }

    private boolean isGatewayOwnerAlive(String owner) {
        Map<Object, Object> agent = stringRedisTemplate.opsForHash().entries("wk:device:gateway:agent:" + owner);
        if (agent.isEmpty()) {
            return false;
        }
        Long lastSeenAt = parseLong(agent.get("lastSeenAt"));
        if (lastSeenAt == null) {
            return false;
        }
        long ttlMs = Math.max(properties.getHeartbeatSeconds() * 3000L, 30000L);
        return System.currentTimeMillis() - lastSeenAt <= ttlMs;
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean requiresLocalEndpoint(DeviceGatewayBinding binding) {
        return binding.getGatewayMode() != com.budwk.sp.device.enums.DeviceGatewayMode.MQTT_CLIENT;
    }
}
