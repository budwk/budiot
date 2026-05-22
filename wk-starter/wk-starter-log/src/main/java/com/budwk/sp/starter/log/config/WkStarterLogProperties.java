package com.budwk.sp.starter.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志 Starter 配置
 *
 * @author wizzer@qq.com
 */
@Data
@ConfigurationProperties(prefix = "wk.log")
public class WkStarterLogProperties {

    private boolean enabled = true;

    private int maxParamsLength = 4000;

    private int maxResultLength = 2000;

    private int maxExceptionLength = 4000;

    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password",
            "salt",
            "token",
            "accessToken",
            "smscode",
            "captchaCode",
            "captchaKey",
            "privateKey",
            "rsaKey"
    ));
}
