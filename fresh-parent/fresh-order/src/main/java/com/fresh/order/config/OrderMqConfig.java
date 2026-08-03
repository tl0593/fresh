package com.fresh.order.config;

import com.fresh.order.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderMqConfig {

    @Bean
    public Consumer<String> orderUnpaidConsumer(OrderService orderService) {
        return orderService::handleUnpaidTimeout;
    }

    @Bean
    public Consumer<String> commentAddConsumer(OrderService orderService) {
        return orderService::handleCommentAdd;
    }
}
