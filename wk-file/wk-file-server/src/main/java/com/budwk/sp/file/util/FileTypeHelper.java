package com.budwk.sp.file.util;

import org.nutz.lang.Strings;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

public final class FileTypeHelper {
    private FileTypeHelper() {
    }

    public static String resolveFileName(MultipartFile file, String category) {
        String originalFilename = Strings.sNull(file.getOriginalFilename()).trim();
        if (Strings.isNotBlank(originalFilename)) {
            return originalFilename;
        }
        String suffix = resolveSuffix(file);
        String filename = "upload";
        if (Strings.isNotBlank(suffix)) {
            filename += "." + suffix;
        }
        if (Strings.isBlank(category)) {
            return filename;
        }
        return category + "-" + filename;
    }

    public static String resolveSuffix(MultipartFile file) {
        String originalFilename = Strings.sNull(file.getOriginalFilename()).trim();
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
            return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }
        return suffixByContentType(resolveContentType(file), "bin");
    }

    public static String resolveContentType(MultipartFile file) {
        if (Strings.isNotBlank(file.getContentType())) {
            return file.getContentType();
        }
        return MediaTypeFactory.getMediaType(resolveFileName(file, "file"))
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    public static boolean isImage(String contentType) {
        return Strings.sNull(contentType).toLowerCase(Locale.ROOT).startsWith("image/");
    }

    public static boolean isAllowed(String category, String contentType) {
        String normalized = Strings.sBlank(category, "file").toLowerCase(Locale.ROOT);
        if ("image".equals(normalized)) {
            return isImage(contentType);
        }
        if ("video".equals(normalized)) {
            return Strings.sNull(contentType).toLowerCase(Locale.ROOT).startsWith("video/");
        }
        return true;
    }

    public static String formatDownloadName(String fileName) {
        return Strings.isBlank(fileName) ? "download.bin" : fileName;
    }

    private static String suffixByContentType(String contentType, String defaultSuffix) {
        if (contentType == null) {
            return defaultSuffix;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_GIF_VALUE -> "gif";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "application/pdf" -> "pdf";
            default -> defaultSuffix;
        };
    }
}
