package com.fresh.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.goods.oss")
public class OssProperties {

    /** 是否启用阿里云 OSS；false 时使用本地存储 */
    private boolean enabled = false;
    private String endpoint = "";
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String bucketName = "";
    /** 本地存储目录（dev 默认） */
    private String localPath = "./uploads";
    /** 访问 URL 前缀（经网关） */
    private String baseUrl = "http://127.0.0.1:8080/api/goods/upload";
}
