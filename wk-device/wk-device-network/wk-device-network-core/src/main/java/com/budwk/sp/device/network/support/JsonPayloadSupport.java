package com.budwk.sp.device.network.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;

import java.util.HashMap;
import java.util.Map;

public final class JsonPayloadSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonPayloadSupport() {}

    public static Map<String, String> extract(String payload) {
        Map<String, String> map = new HashMap<>();
        if (Strings.isBlank(payload) || !payload.trim().startsWith("{")) return map;
        try {
            JsonNode node = OBJECT_MAPPER.readTree(payload);
            put(node, map, "deviceCode");
            put(node, map, "productKey");
            put(node, map, "tenantId");
            put(node, map, "topic");
        } catch (Exception ignored) {
        }
        return map;
    }

    private static void put(JsonNode node, Map<String, String> map, String key) {
        if (node.hasNonNull(key)) map.put(key, node.get(key).asText());
    }
}
