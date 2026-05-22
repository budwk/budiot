package com.budwk.sp.file.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.sys.providers.ISysUserProvider;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @DubboReference(interfaceClass = ISysUserProvider.class, check = false, lazy = true, retries = 0)
    private ISysUserProvider sysUserProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/file/**", r -> StpUtil.checkLogin());
                    SaRouter.match("/pub/file/upload/**", r -> StpUtil.checkLogin());
                }).isAnnotation(true))
                .addPathPatterns("/**");
    }

    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return sysUserProvider.getPermissionList(String.valueOf(loginId));
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return sysUserProvider.getRoleList(String.valueOf(loginId));
            }
        };
    }
}
