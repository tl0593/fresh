package com.fresh.user.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.constant.RocketMQTopicConstant;
import com.fresh.common.dto.mq.IntegralChangeMqDTO;
import com.fresh.common.dto.mq.OrderEventMqDTO;
import com.fresh.common.dto.mq.UserRegisterMqDTO;
import com.fresh.common.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMqProducer {

    private final StreamBridge streamBridge;

    public void sendUserRegister(Long userId, String openid) {
        UserRegisterMqDTO dto = new UserRegisterMqDTO();
        dto.setTraceId(IdUtils.nextIdStr());
        dto.setOperateTime(LocalDateTime.now());
        dto.setUserId(userId);
        dto.setOpenid(openid);
        send("userRegister-out-0", RocketMQTopicConstant.USER_REGISTER_TOPIC, dto);
    }

    public void sendIntegralChange(Long userId, Integer changeNum, Long orderId, String remark) {
        IntegralChangeMqDTO dto = new IntegralChangeMqDTO();
        dto.setTraceId(IdUtils.nextIdStr());
        dto.setOperateUserId(userId);
        dto.setOperateTime(LocalDateTime.now());
        dto.setUserId(userId);
        dto.setChangeNum(changeNum);
        dto.setOrderId(orderId);
        dto.setRemark(remark);
        send("integralChange-out-0", RocketMQTopicConstant.INTEGRAL_CHANGE_TOPIC, dto);
    }

    public void sendUserBehavior(Long userId, Integer behaviorType, Long goodsId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("behaviorType", behaviorType);
        payload.put("goodsId", goodsId);
        payload.put("operateTime", LocalDateTime.now());
        send("userBehavior-out-0", RocketMQTopicConstant.USER_BEHAVIOR_TOPIC, payload);
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
