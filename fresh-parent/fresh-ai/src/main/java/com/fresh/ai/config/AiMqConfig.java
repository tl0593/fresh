package com.fresh.ai.config;

import com.fresh.ai.dto.AfterSaleImageMqDTO;
import com.fresh.ai.service.AiImageRecognizeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class AiMqConfig {

    @Bean
    public Consumer<AfterSaleImageMqDTO> afterSaleImageConsumer(AiImageRecognizeService service) {
        return service::handleAfterSaleImage;
    }
}
