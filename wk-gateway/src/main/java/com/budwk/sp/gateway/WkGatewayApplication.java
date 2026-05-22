package com.budwk.sp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类
 *
 * @author wizzer@qq.com
 */
@SpringBootApplication
@EnableDiscoveryClient
public class WkGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WkGatewayApplication.class, args);
    }
}
