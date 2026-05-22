package com.budwk.sp.starter.web.config;

import com.budwk.sp.starter.web.handler.GlobalExceptionHandler;
import com.budwk.sp.starter.web.log.WebLogAspect;
import com.budwk.sp.starter.web.param.JsonBodyParamFilter;
import com.budwk.sp.starter.web.repeat.RepeatSubmitAspect;
import com.budwk.sp.starter.web.xss.XssFilter;
import com.budwk.sp.starter.web.xss.XssJacksonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@Import({GlobalExceptionHandler.class})
@EnableConfigurationProperties({
        WkLogProperties.class,
        WkExceptionProperties.class,
        WkXssProperties.class
})
public class WkWebAutoConfiguration {


    /**
     * 防重复提交切面
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RepeatSubmitAspect repeatSubmitAspect(StringRedisTemplate stringRedisTemplate) {
        return new RepeatSubmitAspect(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public WebLogAspect webLogAspect(WkLogProperties properties) {
        return new WebLogAspect(properties);
    }

    /**
     * 拦截普通表单和 URL 参数的 XSS
     */
    @Bean
    public FilterRegistrationBean<Filter> jsonBodyParamFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JsonBodyParamFilter(objectMapper));
        registration.addUrlPatterns("/*");
        registration.setName("jsonBodyParamFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "wk.web.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<Filter> xssFilter(WkXssProperties properties) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * 拦截 @RequestBody JSON 的 XSS
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "wk.web.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ObjectMapper wkObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        SimpleModule xssModule = new SimpleModule();
        xssModule.addDeserializer(String.class, new XssJacksonDeserializer());
        mapper.registerModule(xssModule);

        return mapper;
    }
}
