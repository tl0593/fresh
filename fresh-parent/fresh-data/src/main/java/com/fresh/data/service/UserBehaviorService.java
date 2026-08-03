package com.fresh.data.service;

import com.fresh.data.constant.UserBehaviorTypeConstant;
import com.fresh.data.dto.AiChatBehaviorMqDTO;
import com.fresh.data.dto.UserBehaviorMqDTO;
import com.fresh.data.entity.UserBehaviorLog;
import com.fresh.data.mapper.UserBehaviorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorService {

    private static final int BATCH_SIZE = 1000;

    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final DataStatCacheService dataStatCacheService;

    private final BlockingQueue<UserBehaviorLog> buffer = new ArrayBlockingQueue<>(2000);

    public void handleUserBehavior(UserBehaviorMqDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getBehaviorType() == null) {
            return;
        }
        UserBehaviorLog logEntity = toEntity(dto.getUserId(), dto.getBehaviorType(), dto.getGoodsId(), dto.getOperateTime());
        enqueue(logEntity);
        dataStatCacheService.incrementToday(dto.getBehaviorType());
    }

    public void handleAiChatBehavior(AiChatBehaviorMqDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            return;
        }
        UserBehaviorLog logEntity = toEntity(dto.getUserId(), UserBehaviorTypeConstant.AI_CHAT, null, LocalDateTime.now());
        enqueue(logEntity);
        dataStatCacheService.incrementToday(UserBehaviorTypeConstant.AI_CHAT);
    }

    public void handleUserRegister(String payload) {
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        UserBehaviorLog logEntity = toEntity(json.getLong("userId"), UserBehaviorTypeConstant.REGISTER, null, LocalDateTime.now());
        enqueue(logEntity);
        dataStatCacheService.incrementToday(UserBehaviorTypeConstant.REGISTER);
    }

    public void handleCommentAdd(String payload) {
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        UserBehaviorLog logEntity = toEntity(json.getLong("userId"), UserBehaviorTypeConstant.COMMENT,
                json.getLong("goodsId"), LocalDateTime.now());
        enqueue(logEntity);
        dataStatCacheService.incrementToday(UserBehaviorTypeConstant.COMMENT);
    }

    public void handleCouponReceive(String payload) {
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        UserBehaviorLog logEntity = toEntity(json.getLong("userId"), UserBehaviorTypeConstant.COUPON, null, LocalDateTime.now());
        enqueue(logEntity);
        dataStatCacheService.incrementToday(UserBehaviorTypeConstant.COUPON);
    }

    public void flushBuffer() {
        List<UserBehaviorLog> batch = drainBuffer();
        if (CollectionUtils.isEmpty(batch)) {
            return;
        }
        for (UserBehaviorLog row : batch) {
            userBehaviorLogMapper.insert(row);
        }
        log.debug("flushed {} behavior logs", batch.size());
    }

    private void enqueue(UserBehaviorLog logEntity) {
        if (!buffer.offer(logEntity)) {
            flushBuffer();
            buffer.offer(logEntity);
        }
        if (buffer.size() >= BATCH_SIZE) {
            flushBuffer();
        }
    }

    private List<UserBehaviorLog> drainBuffer() {
        if (buffer.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserBehaviorLog> list = new ArrayList<>(buffer.size());
        buffer.drainTo(list);
        return list;
    }

    private UserBehaviorLog toEntity(Long userId, Integer behaviorType, Long goodsId, LocalDateTime operateTime) {
        UserBehaviorLog logEntity = new UserBehaviorLog();
        logEntity.setUserId(userId);
        logEntity.setBehaviorType(behaviorType);
        logEntity.setGoodsId(goodsId);
        logEntity.setCreateTime(operateTime == null ? LocalDateTime.now() : operateTime);
        return logEntity;
    }
}
