package com.fresh.goods.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.constant.RocketMQTopicConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoodsMqProducer {

    private final StreamBridge streamBridge;

    public void sendStockChange(Object payload) {
        send("stockChange-out-0", RocketMQTopicConstant.STOCK_CHANGE_TOPIC, payload);
    }

    public void sendCommentAdd(Object payload) {
        send("commentAdd-out-0", RocketMQTopicConstant.COMMENT_ADD_TOPIC, payload);
    }

    public void sendCouponReceive(Object payload) {
        send("couponReceive-out-0", RocketMQTopicConstant.COUPON_RECEIVE_TOPIC, payload);
    }

    public void sendGroupExpire(Object payload) {
        send("groupExpire-out-0", RocketMQTopicConstant.GROUP_EXPIRE_TOPIC, payload);
    }

    private void send(String binding, String topic, Object payload) {
        try {
            streamBridge.send(binding, JSON.toJSONString(payload));
            log.debug("MQ sent topic={}, payload={}", topic, payload);
        } catch (Exception e) {
            log.warn("MQ send failed, topic={}, payload={}", topic, payload, e);
        }
    }

    public Map<String, Object> buildCouponPayload(Long userId, Long templateId, Long userCouponId) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("templateId", templateId);
        map.put("userCouponId", userCouponId);
        return map;
    }
}
