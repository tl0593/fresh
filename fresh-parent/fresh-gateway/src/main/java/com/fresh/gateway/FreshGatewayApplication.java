package com.fresh.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = "com.fresh.common.config.CommonCoreAutoConfiguration")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.fresh.gateway.mapper")
public class FreshGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshGatewayApplication.class, args);
    }
}
