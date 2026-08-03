package com.fresh.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.gateway")
public class FreshGatewayProperties {

    private String tokenHeader = "Authorization";
    private int limitTimeSecond = 60;
    private boolean accessLogEnable = true;
}
