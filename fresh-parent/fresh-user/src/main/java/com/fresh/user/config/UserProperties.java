package com.fresh.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.user")
public class UserProperties {

    private long miniTokenExpire = 604800;
    private long adminTokenExpire = 7200;
    private long codeExpire = 300;
}
