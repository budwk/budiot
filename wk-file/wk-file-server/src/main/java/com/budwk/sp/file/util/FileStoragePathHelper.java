package com.budwk.sp.file.util;

import org.nutz.lang.Strings;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FileStoragePathHelper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private FileStoragePathHelper() {
    }

    public static String normalizeSubPath(String subPath) {
        String value = Strings.sNull(subPath).trim().replace('\\', '/');
        if (Strings.isBlank(value)) {
            return "";
        }
        String[] segments = value.split("/");
        List<String> normalized = new ArrayList<>();
        for (String segment : segments) {
            String current = Strings.sNull(segment).trim();
            if (Strings.isBlank(current) || ".".equals(current)) {
                continue;
            }
            if ("..".equals(current)) {
                throw new IllegalArgumentException("子目录参数不合法");
            }
            normalized.add(current);
        }
        return String.join("/", normalized);
    }

    public static String buildStoragePath(String tenantId, String suffix, String subPath) {
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (Strings.isNotBlank(suffix)) {
            filename += "." + suffix;
        }
        String normalizedSubPath = normalizeSubPath(subPath);
        if (Strings.isBlank(normalizedSubPath)) {
            return tenantId + "/" + LocalDate.now().format(DATE_FORMATTER) + "/" + filename;
        }
        return tenantId + "/" + normalizedSubPath + "/" + filename;
    }
}
