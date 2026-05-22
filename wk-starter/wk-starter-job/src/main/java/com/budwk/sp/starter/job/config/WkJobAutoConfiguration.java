package com.budwk.sp.starter.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * XXL-JOB 自动配置
 *
 * @author wizzer@qq.com
 */
@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(WkJobProperties.class)
@ConditionalOnProperty(prefix = "wk.job", name = "enabled", havingValue = "true")
public class WkJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobSpringExecutor(WkJobProperties properties) {
        validate(properties);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setTimeout(properties.getTimeout());
        executor.setAppname(properties.getExecutor().getAppname());
        executor.setAddress(properties.getExecutor().getAddress());
        executor.setIp(properties.getExecutor().getIp());
        executor.setPort(properties.getExecutor().getPort());
        executor.setLogPath(properties.getExecutor().getLogPath());
        executor.setLogRetentionDays(properties.getExecutor().getLogRetentionDays());
        if (StringUtils.hasText(properties.getExecutor().getExcludedPackage())) {
            executor.setExcludedPackage(properties.getExecutor().getExcludedPackage());
        }
        return executor;
    }

    private void validate(WkJobProperties properties) {
        if (!StringUtils.hasText(properties.getAdminAddresses())) {
            throw new IllegalArgumentException("wk.job.admin-addresses 不能为空");
        }
        if (!StringUtils.hasText(properties.getExecutor().getAppname())) {
            throw new IllegalArgumentException("wk.job.executor.appname 不能为空");
        }
    }
}
