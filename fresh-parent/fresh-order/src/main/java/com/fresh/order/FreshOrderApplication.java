package com.fresh.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fresh.order.feign")
@MapperScan("com.fresh.order.mapper")
public class FreshOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshOrderApplication.class, args);
    }
}
