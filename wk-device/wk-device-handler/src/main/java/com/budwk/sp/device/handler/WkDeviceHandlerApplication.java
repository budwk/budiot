package com.budwk.sp.device.handler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class
})
@ConfigurationPropertiesScan(basePackages = "com.budwk.sp")
@EnableCaching
public class WkDeviceHandlerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WkDeviceHandlerApplication.class, args);
    }
}
