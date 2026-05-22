package com.budwk.sp.starter.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wk.web.exception")
public class WkExceptionProperties {
    /**
     * 是否将底层异常消息直接返回给前端。
     * 默认关闭，避免将 SQL、堆栈等内部实现细节暴露给客户端。
     */
    private boolean exposeMessage = false;
}
