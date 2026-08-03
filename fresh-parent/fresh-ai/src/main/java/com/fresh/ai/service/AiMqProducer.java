package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.ai.dto.ImageDamageResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMqProducer {

    private final StreamBridge streamBridge;

    public void sendRecognizeFinish(Long afterSaleId, Long userId, ImageDamageResultDTO result) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("afterSaleId", afterSaleId);
        payload.put("userId", userId);
        payload.put("result", result);
        send("aiRecognizeFinish-out-0", payload);
    }

    public void sendChatBehavior(Long userId, String sessionKey, Integer chatType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("sessionKey", sessionKey);
        payload.put("chatType", chatType);
        send("aiChatBehavior-out-0", payload);
    }

    private void send(String binding, Object payload) {
        try {
            streamBridge.send(binding, JSON.toJSONString(payload));
        } catch (Exception e) {
            log.warn("MQ send failed, binding={}, payload={}", binding, payload, e);
        }
    }
}
