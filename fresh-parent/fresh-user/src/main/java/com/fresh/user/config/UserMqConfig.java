package com.fresh.user.config;

import com.fresh.user.service.UserMqConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class UserMqConfig {

    @Bean
    public Consumer<String> orderSuccessConsumer(UserMqConsumerService service) {
        return service::handleOrderSuccess;
    }

    @Bean
    public Consumer<String> afterSaleRefundConsumer(UserMqConsumerService service) {
        return service::handleAfterSaleRefund;
    }
}
