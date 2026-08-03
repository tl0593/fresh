package com.fresh.order.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.constant.RocketMQTopicConstant;
import com.fresh.common.dto.mq.OrderEventMqDTO;
import com.fresh.common.util.IdUtils;
import com.fresh.order.config.OrderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMqProducer {

    private final StreamBridge streamBridge;
    private final OrderProperties orderProperties;

    public void sendOrderCreate(OrderEventMqDTO dto) {
        enrich(dto);
        send("orderCreate-out-0", RocketMQTopicConstant.ORDER_CREATE_TOPIC, dto);
    }

    public void sendOrderUnpaidDelay(OrderEventMqDTO dto) {
        enrich(dto);
        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(dto))
                .setHeader("delayLevel", orderProperties.getUnpaidDelayLevel())
                .build();
        try {
            streamBridge.send("orderUnpaid-out-0", message);
        } catch (Exception e) {
            log.warn("MQ delay send failed ORDER_UNPAID_TOPIC, orderNo={}", dto.getOrderNo(), e);
        }
    }

    public void sendOrderSuccess(OrderEventMqDTO dto) {
        enrich(dto);
        send("orderSuccess-out-0", RocketMQTopicConstant.ORDER_SUCCESS_TOPIC, dto);
    }

    public void sendAfterSaleImage(Long afterSaleId, Long userId, String imageUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("afterSaleId", afterSaleId);
        payload.put("userId", userId);
        payload.put("imageUrl", imageUrl);
        payload.put("traceId", IdUtils.nextIdStr());
        send("afterSaleImage-out-0", RocketMQTopicConstant.AFTER_SALE_IMAGE_TOPIC, payload);
    }

    public void sendOrderCancelStock(Object payload) {
        send("orderCancelStock-out-0", RocketMQTopicConstant.ORDER_CANCEL_STOCK_TOPIC, payload);
    }

    private void enrich(OrderEventMqDTO dto) {
        if (dto.getTraceId() == null) {
            dto.setTraceId(IdUtils.nextIdStr());
        }
        if (dto.getOperateTime() == null) {
            dto.setOperateTime(LocalDateTime.now());
        }
    }

    private void send(String binding, String topic, Object payload) {
        try {
            streamBridge.send(binding, JSON.toJSONString(payload));
            log.debug("MQ sent topic={}, payload={}", topic, payload);
        } catch (Exception e) {
            log.warn("MQ send failed, topic={}, payload={}", topic, payload, e);
        }
    }
}
