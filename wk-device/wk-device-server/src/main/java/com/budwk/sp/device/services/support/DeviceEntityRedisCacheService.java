package com.budwk.sp.device.services.support;

import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.support.DeviceEntityCacheKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeviceEntityRedisCacheService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DeviceEntityRedisCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Device_product getProduct(String tenantId, String productId) {
        return read(DeviceEntityCacheKeys.product(tenantId, productId), Device_product.class);
    }

    public void cacheProduct(Device_product product) {
        if (product != null) {
            write(DeviceEntityCacheKeys.product(product.getTenantId(), product.getId()), product);
        }
    }

    public void evictProduct(String tenantId, String productId) {
        stringRedisTemplate.delete(DeviceEntityCacheKeys.product(tenantId, productId));
    }

    public Device_info getDevice(String tenantId, String deviceId) {
        return read(DeviceEntityCacheKeys.device(tenantId, deviceId), Device_info.class);
    }

    public void cacheDevice(Device_info device) {
        if (device != null) {
            write(DeviceEntityCacheKeys.device(device.getTenantId(), device.getId()), device);
        }
    }

    public void evictDevice(String tenantId, String deviceId) {
        stringRedisTemplate.delete(DeviceEntityCacheKeys.device(tenantId, deviceId));
    }

    public Device_protocol getProtocol(String tenantId, String protocolId) {
        return read(DeviceEntityCacheKeys.protocol(tenantId, protocolId), Device_protocol.class);
    }

    public void cacheProtocol(Device_protocol protocol) {
        if (protocol != null) {
            write(DeviceEntityCacheKeys.protocol(protocol.getTenantId(), protocol.getId()), protocol);
        }
    }

    public void evictProtocol(String tenantId, String protocolId) {
        stringRedisTemplate.delete(DeviceEntityCacheKeys.protocol(tenantId, protocolId));
    }

    private <T> T read(String key, Class<T> type) {
        if (Strings.isBlank(key)) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (Strings.isBlank(json)) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception ignored) {
        }
    }
}
