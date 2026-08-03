package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.fresh.ai.config.AiProperties;
import com.fresh.ai.constant.AiRedisKeyConstant;
import com.fresh.ai.dto.ChatMessageDTO;
import com.fresh.common.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiSessionService {

    private final RedisUtils redisUtils;
    private final AiProperties aiProperties;

    public List<ChatMessageDTO> loadHistory(Long userId, String sessionKey) {
        String json = redisUtils.get(AiRedisKeyConstant.sessionKey(userId, sessionKey));
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        return JSON.parseObject(json, new TypeReference<List<ChatMessageDTO>>() {
        });
    }

    public void saveHistory(Long userId, String sessionKey, List<ChatMessageDTO> history) {
        redisUtils.set(
                AiRedisKeyConstant.sessionKey(userId, sessionKey),
                JSON.toJSONString(history),
                aiProperties.getSessionTtl(),
                TimeUnit.SECONDS
        );
    }

    public void appendMessage(Long userId, String sessionKey, String userMsg, String aiReply) {
        List<ChatMessageDTO> history = loadHistory(userId, sessionKey);
        history.add(new ChatMessageDTO("user", userMsg));
        history.add(new ChatMessageDTO("assistant", aiReply));
        if (history.size() > 20) {
            history = new ArrayList<>(history.subList(history.size() - 20, history.size()));
        }
        saveHistory(userId, sessionKey, history);
    }
}
