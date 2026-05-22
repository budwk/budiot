package com.budwk.sp.starter.dao.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantInterceptor;
import com.budwk.sp.starter.dao.tenant.WkDaoTenantProvider;
import jakarta.annotation.PostConstruct;
import org.nutz.dao.Dao;
import org.nutz.dao.SqlManager;
import org.nutz.dao.impl.FileSqlManager;
import org.nutz.dao.impl.NutDao;
import org.nutz.dao.impl.sql.run.NutDaoRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoConfiguration
@ConditionalOnClass(Dao.class)
@EnableConfigurationProperties(WkDaoTenantProperties.class)
public class WkDaoAutoConfiguration {

    @Autowired
    private Environment env;

    @Autowired
    private GenericApplicationContext applicationContext;

    @Autowired
    private WkDaoTenantProperties tenantProperties;

    // 模拟原有的 PropertiesProxy
    private static final String PROP_INTERCEPTOR_LOG_ENABLE = "wk.dao.interceptor.log.enabled";
    private static final String PROP_INTERCEPTOR_TIME_ENABLE = "wk.dao.interceptor.time.enabled";
    private static final String PROP_SQLS_PATH = "wk.dao.sqls.path";

    @Bean
    public SqlManager sqlManager() {
        String sqlPath = env.getProperty(PROP_SQLS_PATH, "sqls/");
        return new FileSqlManager(sqlPath);
    }

    @PostConstruct
    public void init() {
        String mode = env.getProperty("jdbc.mode", "single");
        if ("many".equalsIgnoreCase(mode)) {
            injectManyDao();
        }
    }

    @Bean(name = "dataSource", destroyMethod = "close")
    @ConditionalOnProperty(name = "jdbc.mode", havingValue = "single", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "dataSource")
    public DataSource dataSource() {
        return createDataSourceByPrefix("jdbc.");
    }

    @Bean(name = "transactionManager")
    @ConditionalOnProperty(name = "jdbc.mode", havingValue = "single", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager tm = new DataSourceTransactionManager(dataSource);
        tm.setEnforceReadOnly(true);
        return tm;
    }

    @Bean(name = "dao")
    @ConditionalOnProperty(name = "jdbc.mode", havingValue = "single", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "dao")
    public Dao dao(DataSource dataSource, SqlManager sqlManager) {
        return createDao(dataSource, sqlManager, "jdbc.");
    }

    /**
     * 动态注入 Dao
     */
    private void injectDao(String beanName, String tmBeanName, String prefix) {
        DataSource dataSource = createDataSourceByPrefix(prefix);

        if (!applicationContext.containsBean(tmBeanName)) {
            applicationContext.registerBean(tmBeanName, PlatformTransactionManager.class, () -> {
                DataSourceTransactionManager tm = new DataSourceTransactionManager(dataSource);
                tm.setEnforceReadOnly(true);
                return tm;
            });
        }
        NutDao dao = createDao(dataSource, applicationContext.getBean(SqlManager.class), prefix);
        applicationContext.registerBean(beanName, NutDao.class, () -> dao);
    }

    private void injectManyDao() {
        String regex = "jdbc\\.many\\.(\\w*)\\.url";
        Pattern pattern = Pattern.compile(regex);

        // 遍历 Environment 中的所有配置 Key
        for (PropertySource<?> ps : applicationContext.getEnvironment().getPropertySources()) {
            if (ps instanceof EnumerablePropertySource) {
                for (String key : ((EnumerablePropertySource<?>) ps).getPropertyNames()) {
                    Matcher match = pattern.matcher(key);
                    if (match.find()) {
                        String name = match.group(1);
                        String prefix = "jdbc.many." + name + ".";

                        // 注册具体名称的 Dao (如 userIdDao)
                        injectDao(name + "Dao", name + "TransactionManager", prefix);

                        // 设置默认 dao
                        String defaultDs = env.getProperty("jdbc.default", "default");
                        if (name.equalsIgnoreCase(defaultDs)) {
                            injectDao("dao", "transactionManager", prefix);
                        }
                    }
                }
            }
        }
    }

    /**
     * 辅助方法：模拟原 DataSourceStarter 的创建行为
     * 实际开发中，建议将其封装为 Spring Bean
     */
    private DataSource createDataSourceByPrefix(String prefix) {
        String configPath = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;

        // 使用 Binder 将配置绑定到 DruidDataSource 实例
        DruidDataSource dataSource = Binder.get(env)
                .bind(configPath, Bindable.of(DruidDataSource.class))
                .orElse(new DruidDataSource());

        // 1. 处理加密滤镜
        String filters = env.getProperty(prefix + "filters", "config"); // 默认尝试开启 config
        try {
            dataSource.setFilters(filters);
        } catch (Exception e) {
            // 如果没有配置 filters，可以忽略或记录日志
        }

        // 2. 注入解密需要的公钥
        // 期望配置格式：jdbc.connection-properties=config.decrypt=true;config.decrypt.key=xxx
        String connectionProps = env.getProperty(prefix + "connection-properties");
        if (connectionProps != null) {
            dataSource.setConnectionProperties(connectionProps);
        }

        // 3. 补足常规参数（兼容你现有的逻辑）
        if (dataSource.getUrl() == null) dataSource.setUrl(env.getProperty(prefix + "url"));
        if (dataSource.getUsername() == null) dataSource.setUsername(env.getProperty(prefix + "username"));
        if (dataSource.getPassword() == null) dataSource.setPassword(env.getProperty(prefix + "password"));

        // Druid 特有的初始化（可选，但在多数据源下建议手动调用以校验配置）
        try {
            dataSource.init();
        } catch (Exception e) {
            throw new RuntimeException("Druid DataSource 启动失败 [" + prefix + "]: " + e.getMessage());
        }

        return dataSource;
    }

    private NutDao createDao(DataSource dataSource, SqlManager sqlManager, String prefix) {
        NutDao dao = new NutDao();
        dao.setDataSource(dataSource);
        dao.setSqlManager(sqlManager);

        List<Object> interceptors = new ArrayList<>();
        if (tenantProperties.isEnabled()) {
            WkDaoTenantProvider tenantProvider = applicationContext.getBeanProvider(WkDaoTenantProvider.class).getIfAvailable();
            interceptors.add(new WkDaoTenantInterceptor(tenantProperties, tenantProvider, env));
        }
        if (env.getProperty(PROP_INTERCEPTOR_LOG_ENABLE, Boolean.class, true)) {
            interceptors.add("log");
        }
        if (env.getProperty(PROP_INTERCEPTOR_TIME_ENABLE, Boolean.class, false)) {
            interceptors.add("time");
        }
        dao.setInterceptors(interceptors);

        String slaveUrlKey = prefix + "slave.url";
        if (env.containsProperty(slaveUrlKey)) {
            DataSource slaveDataSource = createDataSourceByPrefix(prefix + "slave.");
            NutDaoRunner runner = new NutDaoRunner();
            runner.setSlaveDataSource(slaveDataSource);
            dao.setRunner(runner);
        }
        return dao;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({
            ServletRegistrationBean.class,
            FilterRegistrationBean.class,
            StatViewServlet.class,
            WebStatFilter.class
    })
    @ConditionalOnProperty(prefix = "wk.dao.druid-web", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class DruidWebMonitorConfiguration {
        @Bean
        public ServletRegistrationBean<StatViewServlet> druidStatViewServlet(Environment env) {
            ServletRegistrationBean<StatViewServlet> registration = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
            Map<String, String> params = new HashMap<>();
            params.put("loginUsername", env.getProperty("wk.dao.druid.username", "admin"));
            params.put("loginPassword", env.getProperty("wk.dao.druid.password", "admin"));
            params.put("resetEnable", "false");
            registration.setInitParameters(params);
            return registration;
        }

        @Bean
        public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
            FilterRegistrationBean<WebStatFilter> registration = new FilterRegistrationBean<>(new WebStatFilter());
            registration.addUrlPatterns("/*");
            registration.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
            return registration;
        }
    }

}
