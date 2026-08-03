package com.fresh.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "fresh.ai")
public class AiProperties {

    private Llm llm = new Llm();
    private int sessionTtl = 1800;
    private int cookCacheTtl = 3600;
    private int knowledgeCacheTtl = 600;

    @Data
    public static class Llm {
        private String apiKey;
        private String model = "qwen-plus";
        private String visionModel = "qwen-vl-plus";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private int timeout = 30000;
    }
}
