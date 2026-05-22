package com.budwk.sp.starter.log.config;

import com.budwk.sp.starter.log.aspect.SLogAspect;
import com.budwk.sp.starter.log.providers.ISysLogProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * SLog 自动配置
 *
 * @author wizzer@qq.com
 */
@AutoConfiguration
@EnableConfigurationProperties(WkStarterLogProperties.class)
@ConditionalOnProperty(prefix = "wk.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WkLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SLogAspect sLogAspect(ObjectProvider<ObjectMapper> objectMapperProvider,
                                 ObjectProvider<ISysLogProvider> logProvider,
                                 WkStarterLogProperties properties) {
        return new SLogAspect(objectMapperProvider, logProvider, properties);
    }

    @Bean
    @Primary
    @ConditionalOnClass(DubboReference.class)
    @ConditionalOnMissingBean(ISysLogProvider.class)
    public ISysLogProvider remoteSysLogProvider() {
        return new RemoteSysLogProvider();
    }

    static class RemoteSysLogProvider implements ISysLogProvider {

        @DubboReference(interfaceClass = ISysLogProvider.class, check = false, lazy = true, retries = 0)
        private ISysLogProvider sysLogProvider;

        @Override
        public void save(com.budwk.sp.starter.log.model.SLogRecord record) {
            if (sysLogProvider != null) {
                sysLogProvider.save(record);
            }
        }
    }
}
