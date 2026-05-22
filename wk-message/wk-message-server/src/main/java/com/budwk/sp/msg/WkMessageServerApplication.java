package com.budwk.sp.msg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.budwk.sp", exclude = {
        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class
})
@EnableDiscoveryClient
public class WkMessageServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WkMessageServerApplication.class, args);
    }
}
