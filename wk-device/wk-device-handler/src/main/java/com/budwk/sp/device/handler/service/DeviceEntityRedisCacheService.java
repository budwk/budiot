package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
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
public class DeviceEntityRedisCacheService {
    private final Dao dao;
    private final WkCacheService wkCacheService;
    private final ObjectMapper objectMapper;

    public DeviceEntityRedisCacheService(Dao dao, WkCacheService wkCacheService, ObjectMapper objectMapper) {
        this.dao = dao;
        this.wkCacheService = wkCacheService;
        this.objectMapper = objectMapper;
    }

    public Device_info getDevice(String tenantId, String deviceId) {
        Device_info cached = read(DeviceEntityCacheKeys.device(tenantId, deviceId), Device_info.class);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag())) {
            return cached;
        }
        Device_info device = dao.fetch(Device_info.class, Cnd.where("tenantId", "=", tenantId).and("id", "=", deviceId).and("delFlag", "=", false));
        write(DeviceEntityCacheKeys.device(tenantId, deviceId), device);
        return device;
    }

    public Device_product getProduct(String tenantId, String productId) {
        Device_product cached = read(DeviceEntityCacheKeys.product(tenantId, productId), Device_product.class);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag())) {
            return cached;
        }
        Device_product product = dao.fetch(Device_product.class, Cnd.where("tenantId", "=", tenantId).and("id", "=", productId).and("delFlag", "=", false));
        write(DeviceEntityCacheKeys.product(tenantId, productId), product);
        return product;
    }

    public Device_protocol getProtocol(String tenantId, String protocolId) {
        Device_protocol cached = read(DeviceEntityCacheKeys.protocol(tenantId, protocolId), Device_protocol.class);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag()) && !cached.isDisabled()) {
            return cached;
        }
        Device_protocol protocol = dao.fetch(Device_protocol.class, Cnd.where("tenantId", "=", tenantId).and("id", "=", protocolId).and("delFlag", "=", false).and("disabled", "=", false));
        write(DeviceEntityCacheKeys.protocol(tenantId, protocolId), protocol);
        return protocol;
    }

    private <T> T read(String key, Class<T> type) {
        if (Strings.isBlank(key)) {
            return null;
        }
        try {
            String json = Strings.sNull(wkCacheService.getCache(key)).trim();
            if (Strings.isBlank(json)) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String key, Object value) {
        if (Strings.isBlank(key)) {
            return;
        }
        try {
            if (value == null) {
                wkCacheService.deleteCache(key);
            } else {
                long expireSeconds = 3600;
                if (value instanceof Device_protocol) {
                    expireSeconds = 1800;
                } else if (value instanceof Device_product) {
                    expireSeconds = 1800;
                }

                wkCacheService.setCache(key, objectMapper.writeValueAsString(value), expireSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }
}
