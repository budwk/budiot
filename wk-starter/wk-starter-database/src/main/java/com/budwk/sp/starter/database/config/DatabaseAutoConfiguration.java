package com.budwk.sp.starter.database.config;

import com.budwk.sp.starter.dao.config.WkDaoAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.nutz.dao.Dao;
import org.nutz.dao.util.Daos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(WkDaoAutoConfiguration.class)
public class DatabaseAutoConfiguration {
    @Autowired
    private Environment env;

    @Autowired
    private GenericApplicationContext applicationContext;

    // 配置项常量定义
    private static final String PROP_DATABASE_GLOBAL_CHECK = "wk.database.global.checkColumnNameKeyword";
    private static final String PROP_DATABASE_GLOBAL_WRAP = "wk.database.global.forceWrapColumnName";
    private static final String PROP_DATABASE_TABLE_CREATE = "wk.database.table.create";
    private static final String PROP_DATABASE_TABLE_MIGRATION = "wk.database.table.migration";
    private static final String PROP_DATABASE_TABLE_PACKAGE = "wk.database.table.package";

    @PostConstruct
    public void init() {
        // 1. 设置 Nutz Daos 全局静态属性
        Daos.CHECK_COLUMN_NAME_KEYWORD = env.getProperty(PROP_DATABASE_GLOBAL_CHECK, Boolean.class, false);
        Daos.FORCE_WRAP_COLUMN_NAME = env.getProperty(PROP_DATABASE_GLOBAL_WRAP, Boolean.class, false);
        // 其他强制转换配置...
        Daos.FORCE_UPPER_COLUMN_NAME = env.getProperty("wk.database.global.forceUpperColumnName", Boolean.class, false);

        // 3. 执行建表与迁移逻辑
        runTableAction();
    }

    private void runTableAction() {
        // 只有开启了数据库功能才执行
        if (!env.getProperty("wk.database.enabled", Boolean.class, false)) {
            return;
        }

        // 获取主 Dao（通常是名为 "dao" 的 Bean）
        if (!applicationContext.containsBean("dao")) {
            return;
        }

        Dao dao = applicationContext.getBean("dao", Dao.class);

        List<String> pkgList = Binder.get(env)
                .bind(PROP_DATABASE_TABLE_PACKAGE, Bindable.listOf(String.class))
                .orElse(Collections.emptyList());

        if (pkgList.isEmpty()) return;

        boolean isCreate = env.getProperty(PROP_DATABASE_TABLE_CREATE, Boolean.class, false);
        boolean isMigration = env.getProperty(PROP_DATABASE_TABLE_MIGRATION, Boolean.class, false);

        for (String pkg : pkgList) {
            String trimmedPkg = pkg.trim();
            // 自动建表
            if (isCreate) {
                Daos.createTablesInPackage(dao, trimmedPkg, false);
            }
            // 自动迁移（表结构变更）
            if (isMigration) {
                Daos.migration(dao, trimmedPkg,
                        (boolean) env.getProperty("wk.database.table.add", Boolean.class, true),
                        (boolean) env.getProperty("wk.database.table.delete", Boolean.class, false),
                        (boolean) env.getProperty("wk.database.table.check", Boolean.class, false)
                );
            }
        }
    }
}
