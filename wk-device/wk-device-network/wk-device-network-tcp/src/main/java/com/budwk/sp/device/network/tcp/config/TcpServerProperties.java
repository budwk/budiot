package com.budwk.sp.device.network.tcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wk.device.network.tcp")
public class TcpServerProperties {
    /**
     * TCP 连接空闲超时时间（秒），0 表示不启用超时关闭。
     */
    private int idleTimeoutSeconds = 60;
}
