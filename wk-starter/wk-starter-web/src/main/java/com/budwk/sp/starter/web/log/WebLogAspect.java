package com.budwk.sp.starter.web.log;

import com.budwk.sp.starter.web.config.WkLogProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class WebLogAspect {
    private final WkLogProperties properties; // 注入配置类
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 扫描所有控制器的公共方法
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public void webLog() {
    }

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 获取当前请求详情
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 计算耗时
        long executionTime = System.currentTimeMillis() - startTime;
        if (properties.isEnabled()) {
            String requestUri = resolveRequestUri(request);
            // 打印结构化日志
            log.debug("--------------------------------------------------");
            log.debug("URL    : {} {}", request.getMethod(), request.getRequestURL().toString());
            log.debug("Class  : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            log.debug("Args   : {}", shouldIgnoreArgs(requestUri) ? "[SKIPPED]" : Arrays.toString(joinPoint.getArgs()));
            log.debug("Time   : {} ms", executionTime);
            log.debug("--------------------------------------------------");
        }
        if (executionTime > properties.getSlowThreshold()) {
            log.warn("检测到慢请求！耗时: {} {} {} ms", request.getMethod(), request.getRequestURL().toString(), executionTime);
        }
        return result;
    }

    private boolean shouldIgnoreArgs(String requestUri) {
        if (!StringUtils.hasText(requestUri) || properties.getIgnoreArgPaths() == null || properties.getIgnoreArgPaths().isEmpty()) {
            return false;
        }
        return properties.getIgnoreArgPaths().stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), requestUri));
    }

    private String resolveRequestUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
