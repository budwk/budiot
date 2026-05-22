package com.budwk.sp.sys.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.sys.services.SysUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/sys/**", r -> StpUtil.checkLogin());
                    SaRouter.match("/auth/info", r -> StpUtil.checkLogin());
                    SaRouter.match("/auth/checkpwd", r -> StpUtil.checkLogin());
                    SaRouter.match("/auth/theme", r -> StpUtil.checkLogin());
                    SaRouter.match("/auth/logout", r -> StpUtil.checkLogin());
                }).isAnnotation(true))
                .addPathPatterns("/**");
    }

    @Bean
    public StpInterface stpInterface(SysUserService sysUserService) {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return sysUserService.getPermissionList(String.valueOf(loginId));
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return sysUserService.getRoleList(String.valueOf(loginId));
            }
        };
    }
}
