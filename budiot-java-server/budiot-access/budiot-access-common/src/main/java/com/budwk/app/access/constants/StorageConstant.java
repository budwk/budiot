package com.budwk.app.access.constants;

public class StorageConstant {
    /**
     * Tdengine配置文件前缀
     */
    public static final String JDBC_TDENGINE_PREFIX = "jdbc.taos.";

    /**
     * 分割
     */
    public static final String UNDER_LINE = "_";

    /**
     * 空格
     */
    public static final String BLANK = " ";

    /**
     * 配置信息前缀
     */
    public static final String CONFIG_PREFIX = "device.storage.ttl";
    /**
     * 通讯报文保存时间
     */
    public static final String RAW_DATA_TTL_CONFIG_KEY = CONFIG_PREFIX + "raw";
    public static final String EVENT_DATA_TTL_CONFIG_KEY = CONFIG_PREFIX + "event";
    /**
     * 通讯报文保存默认时间 30天
     */
    public static final long RAW_DATA_TTL_CONFIG_DEFAULT = 30;
    /**
     * 事件数据默认存储时间 90天
     */
    public static final long EVENT_DATA_TTL_CONFIG_DEFAULT = 90;
    /**
     * 上报数据保存时间
     */
    public static final String DATA_TTL_CONFIG_KEY = CONFIG_PREFIX + "data";
    /**
     * 上报数据保存默认时间 0=永久
     */
    public static final long DATA_TTL_CONFIG_DEFAULT = 0;
}
