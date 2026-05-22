package com.budwk.sp.device.gateway.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.nutz.lang.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnProperty(prefix = "wk.device.database-ext", name = "mongo-enabled", havingValue = "true")
public class MongoDatabaseConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public MongoClient mongoClient(GatewayArchiveProperties properties, Environment environment) {
        return MongoClients.create(resolveUri(properties, environment));
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient, GatewayArchiveProperties properties, Environment environment) {
        String uri = resolveUri(properties, environment);
        String database = new ConnectionString(uri).getDatabase();
        if (Strings.isBlank(database)) {
            throw new IllegalStateException("MongoDB connection is missing database name");
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    private String resolveUri(GatewayArchiveProperties properties, Environment environment) {
        String uri = Strings.sBlank(properties.getMongoUri(), environment.getProperty("spring.data.mongodb.uri"));
        if (Strings.isBlank(uri)) {
            throw new IllegalStateException("MongoDB is enabled but no connection URI is configured");
        }
        return uri;
    }
}
