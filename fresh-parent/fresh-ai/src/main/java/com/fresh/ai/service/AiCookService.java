package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.ai.client.DashScopeClient;
import com.fresh.ai.config.AiProperties;
import com.fresh.ai.constant.AiRedisKeyConstant;
import com.fresh.ai.vo.CookHistoryVO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiCookService {

    private static final String COOK_PROMPT = """
            你是社区生鲜平台的AI营养师，请根据用户偏好生成：
            1. 一周采购清单（按天列出）
            2. 3套配套家常菜谱（含食材用量和简要步骤）
            回答使用中文，结构清晰。
            """;

    private final DashScopeClient dashScopeClient;
    private final RedisUtils redisUtils;
    private final AiProperties aiProperties;

    public String generateCook(String preference) {
        Long userId = requireUserId();
        String prompt = StringUtils.hasText(preference)
                ? COOK_PROMPT + "\n用户偏好：" + preference
                : COOK_PROMPT + "\n用户暂无特殊偏好，请给出均衡家常方案。";
        String content;
        try {
            content = dashScopeClient.chatWithSystem(prompt, List.of());
        } catch (BusinessException e) {
            content = "【推荐菜谱】清炒时蔬、番茄牛腩、杂粮饭。\n【采购清单】时令蔬菜、牛腩500g、杂粮米1kg。";
        }
        cacheCook(userId, content);
        return content;
    }

    public CookHistoryVO getCookHistory() {
        Long userId = requireUserId();
        String json = redisUtils.get(AiRedisKeyConstant.cookKey(userId));
        CookHistoryVO vo = new CookHistoryVO();
        vo.setUserId(userId);
        if (!StringUtils.hasText(json)) {
            vo.setContent(null);
            return vo;
        }
        CookHistoryVO cached = JSON.parseObject(json, CookHistoryVO.class);
        vo.setContent(cached.getContent());
        vo.setCacheTime(cached.getCacheTime());
        return vo;
    }

    public List<String> batchGenerateCook(List<Long> goodsIdList) {
        List<String> result = new ArrayList<>();
        if (goodsIdList == null || goodsIdList.isEmpty()) {
            return result;
        }
        for (Long goodsId : goodsIdList) {
            try {
                String prompt = COOK_PROMPT + "\n重点商品ID：" + goodsId + "，请围绕该生鲜给出1套菜谱。";
                result.add(dashScopeClient.chatWithSystem(prompt, List.of()));
            } catch (BusinessException e) {
                result.add("商品" + goodsId + "配套菜谱：清炒时蔬，简单快手。");
            }
        }
        return result;
    }

    private void cacheCook(Long userId, String content) {
        CookHistoryVO vo = new CookHistoryVO();
        vo.setUserId(userId);
        vo.setContent(content);
        vo.setCacheTime(System.currentTimeMillis());
        redisUtils.set(
                AiRedisKeyConstant.cookKey(userId),
                JSON.toJSONString(vo),
                aiProperties.getCookCacheTtl(),
                TimeUnit.SECONDS
        );
    }

    private Long requireUserId() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
