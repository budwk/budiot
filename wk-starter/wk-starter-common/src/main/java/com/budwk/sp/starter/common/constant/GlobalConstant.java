package com.budwk.sp.starter.common.constant;

public class GlobalConstant {
    /**
     * 软件版本号
     */
    public static final String VERSION = "2.0.0";

    public static final String CACHE_PREFIX = "wk:sp:";

    /**
     * 默认时间格式
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 微服务之间传递的唯一标识 header
     */
    public static final String WK_TRACE_ID = "wk-trace-id";

    /**
     * header 中租户ID
     */
    public static final String WK_TENANT_ID = "wk-tenant-id";

    /**
     * 默认租户ID
     */
    public static final String TENANT_ID_DEFAULT = "platform";


    /**
     * X_POWER_BY
     */
    public static final String X_POWER_BY = "BUDIOT V2 <budwk.com>";

    /**
     * json类型报文
     */
    public static final String JSON_UTF8 = "application/json;charset=UTF-8";

    /**
     * 默认系统管理员角色
     */
    public static final String DEFAULT_SYSADMIN_ROLECODE = "sysadmin";

    /**
     * 默认系统管理员用户名
     */
    public static final String DEFAULT_SYSADMIN_LOGINNAME = "superadmin";

    /**
     * 用户Token参数名
     */
    public static final String HEADER_TOKEN = "Authorization";
    /**
     * 公共应用ID
     */
    public static final String DEFAULT_COMMON_APPID = "COMMON";
    /**
     * 控制中心应用ID
     */
    public static final String DEFAULT_PLATFORM_APPID = "PLATFORM";

}
