package com.budwk.sp.starter.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "wk.web.log")
public class WkLogProperties {
    private boolean enabled = true;
    private long slowThreshold = 1000; // 超过1000ms才打印警告日志
    private List<String> ignoreArgPaths = new ArrayList<>();
}
