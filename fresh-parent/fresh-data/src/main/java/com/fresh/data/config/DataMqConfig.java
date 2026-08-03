package com.fresh.data.config;

import com.fresh.data.dto.AiChatBehaviorMqDTO;
import com.fresh.data.dto.UserBehaviorMqDTO;
import com.fresh.data.service.UserBehaviorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class DataMqConfig {

    @Bean
    public Consumer<UserBehaviorMqDTO> userBehaviorConsumer(UserBehaviorService userBehaviorService) {
        return userBehaviorService::handleUserBehavior;
    }

    @Bean
    public Consumer<AiChatBehaviorMqDTO> aiChatBehaviorConsumer(UserBehaviorService userBehaviorService) {
        return userBehaviorService::handleAiChatBehavior;
    }

    @Bean
    public Consumer<String> userRegisterConsumer(UserBehaviorService userBehaviorService) {
        return userBehaviorService::handleUserRegister;
    }

    @Bean
    public Consumer<String> commentAddConsumer(UserBehaviorService userBehaviorService) {
        return userBehaviorService::handleCommentAdd;
    }

    @Bean
    public Consumer<String> couponReceiveConsumer(UserBehaviorService userBehaviorService) {
        return userBehaviorService::handleCouponReceive;
    }
}
