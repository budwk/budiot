package com.budwk.sp.starter.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * XXL-JOB 配置属性
 *
 * @author wizzer@qq.com
 */
@Data
@ConfigurationProperties(prefix = "wk.job")
public class WkJobProperties {

    /**
     * 是否启用 XXL-JOB 执行器自动配置
     */
    private boolean enabled = false;

    /**
     * 调度中心地址，多个地址用逗号分隔
     */
    private String adminAddresses;

    /**
     * 调度中心通讯令牌
     */
    private String accessToken;

    /**
     * 调度请求超时时间（秒）
     */
    private int timeout = 3;

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();

    @Data
    public static class Executor {
        /**
         * 执行器名称，需要与调度中心配置一致
         */
        private String appname;

        /**
         * 执行器注册地址，通常留空自动注册
         */
        private String address;

        /**
         * 执行器 IP，留空自动获取
         */
        private String ip;

        /**
         * 执行器端口，0 表示自动分配
         */
        private int port = 0;

        /**
         * 日志目录
         */
        private String logPath = "logs/xxl-job/jobhandler";

        /**
         * 日志保留天数
         */
        private int logRetentionDays = 30;

        /**
         * 排除扫描包，多个包路径用逗号分隔
         */
        private String excludedPackage;
    }
}
