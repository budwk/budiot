package com.budwk.sp.device.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DeviceThingPropertySupport {
    private DeviceThingPropertySupport() {
    }

    public static List<Map<String, Object>> parseProperties(String raw, ObjectMapper objectMapper) {
        if (Strings.isBlank(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String readIdentifier(Map<String, Object> property) {
        return readString(property, "identifier");
    }

    public static String readName(Map<String, Object> property) {
        return readString(property, "name");
    }

    public static String readUnit(Map<String, Object> property) {
        return readString(property, "unit");
    }

    public static String toColumnName(String identifier) {
        return "p_" + sanitizeIdentifier(identifier);
    }

    public static String toLegacyColumnName(String identifier) {
        String safe = sanitizeIdentifier(identifier);
        String hash = Integer.toHexString(Strings.sBlank(identifier, "default").hashCode());
        if (hash.length() > 8) {
            hash = hash.substring(hash.length() - 8);
        }
        return "p_" + safe + "_" + hash;
    }

    public static String normalizeValue(String valueJson, ObjectMapper objectMapper) {
        if (Strings.isBlank(valueJson)) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            if (node == null || node.isNull()) {
                return "";
            }
            if (node.isTextual()) {
                return node.asText();
            }
            if (node.isNumber() || node.isBoolean()) {
                return node.asText();
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return Strings.sNull(valueJson).trim();
        }
    }

    private static String sanitizeIdentifier(String value) {
        String safe = Strings.sBlank(value, "default").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }

    private static String readString(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key) || source.get(key) == null) {
            return "";
        }
        return Strings.sNull(String.valueOf(source.get(key))).trim();
    }
}
