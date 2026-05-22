package com.budwk.sp.file.enums;

public enum FileStorageType {
    LOCAL("LOCAL", "本地"),
    FTP("FTP", "FTP"),
    FDFS("FDFS", "FastDFS"),
    MINIO("MINIO", "MinIO");

    private final String value;
    private final String text;

    FileStorageType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static FileStorageType fromValue(String value) {
        for (FileStorageType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的文件存储类型: " + value);
    }
}
