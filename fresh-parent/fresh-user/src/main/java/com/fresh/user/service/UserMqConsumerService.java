package com.fresh.user.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.dto.mq.OrderEventMqDTO;
import com.fresh.user.dto.IntegralDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMqConsumerService {

    private final IntegralService integralService;
    private final UserMqProducer userMqProducer;

    public void handleOrderSuccess(String payload) {
        OrderEventMqDTO dto = JSON.parseObject(payload, OrderEventMqDTO.class);
        if (dto == null || dto.getUserId() == null) {
            return;
        }
        // 积分奖励改在订单核销完成时由 order 服务 Feign 发放；此处仅记录支付事件
        log.info("消费 ORDER_SUCCESS_TOPIC(不发积分), orderNo={}, userId={}, payAmount={}",
                dto.getOrderNo(), dto.getUserId(), dto.getPayAmount());
    }

    public void handleAfterSaleRefund(String payload) {
        Map<?, ?> map = JSON.parseObject(payload, Map.class);
        if (map == null || map.get("userId") == null) {
            return;
        }
        Long userId = Long.valueOf(map.get("userId").toString());
        Integer refundIntegral = map.get("refundIntegral") != null
                ? Integer.valueOf(map.get("refundIntegral").toString()) : 0;
        if (refundIntegral <= 0) {
            return;
        }
        log.info("消费 AFTER_SALE_REFUND_TOPIC, userId={}, refundIntegral={}", userId, refundIntegral);
        IntegralDTO dto = new IntegralDTO();
        dto.setUserId(userId);
        dto.setIntegral(-refundIntegral);
        dto.setRemark("售后退款扣回积分");
        integralService.change(dto);
        userMqProducer.sendIntegralChange(userId, -refundIntegral, null, "售后退款扣回积分");
    }
}
