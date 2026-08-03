package com.fresh.goods.config;

import com.fresh.goods.dto.StockChangeDTO;
import com.fresh.goods.service.GoodsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class GoodsMqConfig {

    @Bean
    public Consumer<StockChangeDTO> orderCancelStockConsumer(GoodsService goodsService) {
        return goodsService::restoreStock;
    }

    @Bean
    public Consumer<Map<String, Object>> groupExpireStatusConsumer(GoodsService goodsService) {
        return goodsService::handleGroupExpire;
    }
}
