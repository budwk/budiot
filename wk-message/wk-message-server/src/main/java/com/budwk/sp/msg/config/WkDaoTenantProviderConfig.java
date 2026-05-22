package com.budwk.sp.msg.config;

import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantProvider;
import org.nutz.lang.Strings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WkDaoTenantProviderConfig {
    @Bean
    public WkDaoTenantProvider wkDaoTenantProvider() {
        return () -> {
            try {
                if (!StpUtil.isLogin()) {
                    return null;
                }
                return Strings.sBlank(StpUtil.getSession().getString("tenantId"), GlobalConstant.TENANT_ID_DEFAULT);
            } catch (SaTokenContextException e) {
                return null;
            }
        };
    }
}
