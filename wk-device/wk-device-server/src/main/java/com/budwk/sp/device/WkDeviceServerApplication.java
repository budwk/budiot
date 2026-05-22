package com.budwk.sp.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class
})
@EnableAsync
@EnableDiscoveryClient
@EnableCaching
@Slf4j
public class WkDeviceServerApplication {
    public static void main(String[] args) { SpringApplication.run(WkDeviceServerApplication.class, args); }
}
