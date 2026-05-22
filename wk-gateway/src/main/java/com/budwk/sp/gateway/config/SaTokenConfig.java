package com.budwk.sp.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Sa-Token 权限认证配置 (Spring Boot 4 / WebFlux 版)
 */
@Configuration
@RefreshScope // 支持 Nacos 配置动态刷新
public class SaTokenConfig {

    private final WkGatewayProperties wkGatewayProperties;

    // 构造函数注入（Spring 推荐做法）
    public SaTokenConfig(WkGatewayProperties wkGatewayProperties) {
        this.wkGatewayProperties = wkGatewayProperties;
    }

    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 1. 指定 [拦截路由]
                .addInclude("/**")

                // 2. 指定 [放行路由] (从 Nacos 读取的动态列表)
                .addExclude(wkGatewayProperties.getIgnoreUrls().toArray(new String[0]))

                // 3. 指定 [认证函数]: 每次请求执行
                .setAuth(obj -> {
                    if (SaHolder.getRequest().isMethod("OPTIONS")) {
                        return;
                    }
                    // 登录校验：除放行路径外，其余均需登录
                    // 对于 IoT 平台，建议后续在此处扩展 [角色/权限] 校验
                    SaRouter.match("/**", r -> StpUtil.checkLogin());
                })

                // 4. 指定 [异常处理函数]: 针对未登录等情况返回 JSON
                .setError(e -> {
                    String origin = SaHolder.getRequest().getHeader("Origin");
                    if (origin != null && !origin.isBlank()) {
                        SaHolder.getResponse().setHeader("Vary", "Origin");
                        SaHolder.getResponse().setHeader("Access-Control-Allow-Origin", origin);
                        SaHolder.getResponse().setHeader("Access-Control-Allow-Credentials", "true");
                    }
                    SaHolder.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
                    // 自动设置JSON响应头，无需手动操作Response，避免Netty协议异常
                    return SaResult.error(e.getMessage())
                            .setCode(HttpStatus.UNAUTHORIZED.value());
                });
    }


    /**
     * 对 actuator 健康检查接口 做账号密码鉴权
     */
    @Bean
    public SaReactorFilter actuatorFilter() {
        String username = wkGatewayProperties.getActuator().getUsername();
        String password = wkGatewayProperties.getActuator().getPassword();
        return new SaReactorFilter()
                .addInclude("/actuator", "/actuator/**")
                .setAuth(obj -> {
                    SaHttpBasicUtil.check(username + ":" + password);
                })
                .setError(e -> {
                    SaHolder.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
                    return SaResult.error(e.getMessage())
                            .setCode(HttpStatus.UNAUTHORIZED.value());
                });
    }
}
