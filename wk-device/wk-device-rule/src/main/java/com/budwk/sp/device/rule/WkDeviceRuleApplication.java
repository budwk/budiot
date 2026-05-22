package com.budwk.sp.device.rule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.budwk.sp")
@ConfigurationPropertiesScan(basePackages = "com.budwk.sp")
public class WkDeviceRuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(WkDeviceRuleApplication.class, args);
    }
}
