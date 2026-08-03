package com.fresh.ai;

import com.fresh.ai.config.AiProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(AiProperties.class)
@MapperScan("com.fresh.ai.mapper")
public class FreshAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshAiApplication.class, args);
    }
}
