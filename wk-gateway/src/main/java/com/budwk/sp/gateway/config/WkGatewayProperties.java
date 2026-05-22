package com.budwk.sp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关属性配置
 *
 * @author wizzer@qq.com
 */
@Data
@Component
@ConfigurationProperties(prefix = "wk.gateway")
public class WkGatewayProperties {

    private List<String> ignoreUrls = new ArrayList<>();

    /**
     * Actuator 监控配置
     */
    private Actuator actuator = new Actuator();

    /**
     * 链路追踪配置
     */
    private Trace trace = new Trace();

    /**
     * 请求日志配置
     */
    private Log log = new Log();

    /**
     * 跨域配置
     */
    private Cors cors = new Cors();

    @Data
    public static class Trace {
        private boolean enabled = true;
        private boolean responseHeader = true;
    }

    @Data
    public static class Log {
        private boolean enabled = true;
    }

    @Data
    public static class Actuator {
        private String username = "admin";
        private String password = "admin";
    }

    @Data
    public static class Cors {
        private boolean enabled = true;
    }
}
