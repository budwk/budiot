package com.budwk.app.access.processor.core;

import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.iot.objects.cache.DeviceProcessCache;
import lombok.Data;
import org.nutz.castor.Castors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ProcessorContext {
    private final DeviceProcessCache device;
    /**
     * 解析后的数据
     */
    private final DefaultDecodeResult decodeResult;
    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    public ProcessorContext addProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public <T> T getProperty(String key, Class<T> type) {
        return Castors.me().castTo(getProperty(key), type);
    }

    public void addProperties(Map<String, String> values) {
        properties.putAll(values);
    }
}
