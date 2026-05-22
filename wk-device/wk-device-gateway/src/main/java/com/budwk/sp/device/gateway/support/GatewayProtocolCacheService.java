package com.budwk.sp.device.gateway.support;

import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.support.DeviceEntityCacheKeys;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class GatewayProtocolCacheService {
    private final Dao dao;
    private final WkCacheService wkCacheService;
    private final ObjectMapper objectMapper;

    public GatewayProtocolCacheService(Dao dao, WkCacheService wkCacheService, ObjectMapper objectMapper) {
        this.dao = dao;
        this.wkCacheService = wkCacheService;
        this.objectMapper = objectMapper;
    }

    public Device_protocol getProtocol(String tenantId, String protocolId) {
        String key = DeviceEntityCacheKeys.protocol(tenantId, protocolId);
        Device_protocol cached = read(key);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag()) && !cached.isDisabled()) {
            return cached;
        }
        Device_protocol protocol = dao.fetch(Device_protocol.class, Cnd.where("tenantId", "=", tenantId)
                .and("id", "=", protocolId)
                .and("delFlag", "=", false)
                .and("disabled", "=", false));
        write(key, protocol);
        return protocol;
    }

    private Device_protocol read(String key) {
        try {
            String json = Strings.sNull(wkCacheService.getCache(key)).trim();
            return Strings.isBlank(json) ? null : objectMapper.readValue(json, Device_protocol.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String key, Device_protocol protocol) {
        try {
            if (protocol == null) {
                wkCacheService.deleteCache(key);
            } else {
                wkCacheService.setCache(key, objectMapper.writeValueAsString(protocol), 1800, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }
}
