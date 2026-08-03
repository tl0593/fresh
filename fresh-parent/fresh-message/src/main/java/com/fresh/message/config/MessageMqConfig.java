package com.fresh.message.config;

import com.fresh.message.service.MessagePushService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class MessageMqConfig {

    @Bean
    public Consumer<String> orderCreateConsumer(MessagePushService service) {
        return service::handleOrderCreate;
    }

    @Bean
    public Consumer<String> orderSuccessConsumer(MessagePushService service) {
        return service::handleOrderSuccess;
    }

    @Bean
    public Consumer<String> orderUnpaidConsumer(MessagePushService service) {
        return service::handleOrderUnpaid;
    }

    @Bean
    public Consumer<String> integralChangeConsumer(MessagePushService service) {
        return service::handleIntegralChange;
    }

    @Bean
    public Consumer<String> couponReceiveConsumer(MessagePushService service) {
        return service::handleCouponReceive;
    }

    @Bean
    public Consumer<String> commentAddConsumer(MessagePushService service) {
        return service::handleCommentAdd;
    }

    @Bean
    public Consumer<String> aiRecognizeFinishConsumer(MessagePushService service) {
        return service::handleAiRecognizeFinish;
    }
}
