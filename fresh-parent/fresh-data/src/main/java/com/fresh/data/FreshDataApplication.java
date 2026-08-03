package com.fresh.data;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fresh.data.feign")
@EnableScheduling
@MapperScan("com.fresh.data.mapper")
public class FreshDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshDataApplication.class, args);
    }
}
