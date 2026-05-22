package com.budwk.sp.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class
})
@EnableAsync
@EnableDiscoveryClient
public class WkFileServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WkFileServerApplication.class, args);
    }
}
