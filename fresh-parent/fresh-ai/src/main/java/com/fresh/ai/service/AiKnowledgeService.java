package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.ai.config.AiProperties;
import com.fresh.ai.constant.AiRedisKeyConstant;
import com.fresh.ai.entity.AiKnowledge;
import com.fresh.ai.mapper.AiKnowledgeMapper;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiKnowledgeService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final RedisUtils redisUtils;
    private final AiProperties aiProperties;

    public List<AiKnowledge> listKnowledge() {
        return knowledgeMapper.selectList(new LambdaQueryWrapper<AiKnowledge>()
                .eq(AiKnowledge::getStatus, 1)
                .orderByAsc(AiKnowledge::getSort)
                .orderByDesc(AiKnowledge::getId));
    }

    public AiKnowledge getKnowledge(Long id) {
        AiKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST);
        }
        return knowledge;
    }

    public void saveKnowledge(AiKnowledge knowledge) {
        knowledge.setId(null);
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        if (knowledge.getSort() == null) {
            knowledge.setSort(0);
        }
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        clearCache();
    }

    public void updateKnowledge(AiKnowledge knowledge) {
        if (knowledge.getId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST);
        }
        knowledgeMapper.updateById(knowledge);
        clearCache();
    }

    public void deleteKnowledge(Long id) {
        knowledgeMapper.deleteById(id);
        clearCache();
    }

    public String matchKnowledge(String userMsg) {
        List<AiKnowledge> list = loadCachedKnowledge();
        for (AiKnowledge item : list) {
            if (userMsg.contains(item.getQuestion())) {
                return item.getAnswer();
            }
        }
        return null;
    }

    private List<AiKnowledge> loadCachedKnowledge() {
        String cached = redisUtils.get(AiRedisKeyConstant.KNOWLEDGE_LIST);
        if (StringUtils.hasText(cached)) {
            return JSON.parseArray(cached, AiKnowledge.class);
        }
        List<AiKnowledge> list = knowledgeMapper.selectList(new LambdaQueryWrapper<AiKnowledge>()
                .eq(AiKnowledge::getStatus, 1)
                .orderByAsc(AiKnowledge::getSort));
        redisUtils.set(
                AiRedisKeyConstant.KNOWLEDGE_LIST,
                JSON.toJSONString(list),
                aiProperties.getKnowledgeCacheTtl(),
                TimeUnit.SECONDS
        );
        return list;
    }

    private void clearCache() {
        redisUtils.delete(AiRedisKeyConstant.KNOWLEDGE_LIST);
    }
}
