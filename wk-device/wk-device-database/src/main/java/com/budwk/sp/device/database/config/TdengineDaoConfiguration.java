package com.budwk.sp.device.database.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.nutz.dao.Dao;
import org.nutz.dao.SqlManager;
import org.nutz.dao.impl.NutDao;
import org.nutz.lang.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TdengineDaoConfiguration {
    @Bean(name = "tdengineDataSource", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "wk.device.database-ext", name = "tdengine-enabled", havingValue = "true")
    public DruidDataSource tdengineDataSource(DeviceDatabaseProperties properties) throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getTdengineUrl());
        dataSource.setUsername(properties.getTdengineUsername());
        dataSource.setPassword(properties.getTdenginePassword());
        if (Strings.isNotBlank(properties.getTdengineDriverClassName())) {
            dataSource.setDriverClassName(properties.getTdengineDriverClassName());
        }
        dataSource.init();
        return dataSource;
    }

    @Bean(name = "tdengineDao")
    @ConditionalOnProperty(prefix = "wk.device.database-ext", name = "tdengine-enabled", havingValue = "true")
    public Dao tdengineDao(DruidDataSource tdengineDataSource, SqlManager sqlManager) {
        NutDao dao = new NutDao();
        dao.setDataSource(tdengineDataSource);
        dao.setSqlManager(sqlManager);
        return dao;
    }
}
