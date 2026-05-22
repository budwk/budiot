package com.budwk.sp.starter.dao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "wk.dao.tenant")
public class WkDaoTenantProperties {
    /**
     * 是否启用租户数据隔离
     */
    private boolean enabled;

    /**
     * 忽略租户过滤的表名
     */
    private List<String> ignoreTables = new ArrayList<>();
}
