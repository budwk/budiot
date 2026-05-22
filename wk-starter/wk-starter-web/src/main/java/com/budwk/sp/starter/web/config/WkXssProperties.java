package com.budwk.sp.starter.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "wk.web.xss")
public class WkXssProperties {
    /**
     * 是否开启 XSS 过滤
     */
    private boolean enabled = true;

    /**
     * 排除名单（支持 AntPath 风格，如 /sys/**）
     */
    private List<String> excludeUrls = new ArrayList<>();
}