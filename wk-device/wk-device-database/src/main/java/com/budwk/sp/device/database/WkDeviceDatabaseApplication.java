package com.budwk.sp.device.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class
})
@ConfigurationPropertiesScan(basePackages = "com.budwk.sp")
public class WkDeviceDatabaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(WkDeviceDatabaseApplication.class, args);
    }
}
