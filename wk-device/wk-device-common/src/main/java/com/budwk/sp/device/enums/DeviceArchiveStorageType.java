package com.budwk.sp.device.enums;

/**
 * 设备归档数据存储类型。
 */
public enum DeviceArchiveStorageType {
    /**
     * 默认存储。
     * <p>
     * 表示使用当前业务主数据源对应的默认关系库，如 PostgreSQL / MySQL。
     */
    DEFAULT,
    /**
     * MongoDB 文档存储。
     */
    MONGODB,
    /**
     * TDengine 时序数据库。
     */
    TDENGINE
}
