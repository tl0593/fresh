package com.fresh.data.service;

import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.data.config.DataProperties;
import com.fresh.data.constant.DataRedisKeyConstant;
import com.fresh.data.vo.TodayStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DataStatCacheService {

    private final RedisUtils redisUtils;
    private final DataProperties dataProperties;

    public TodayStatVO getTodayStatFromCache() {
        String json = redisUtils.get(DataRedisKeyConstant.TODAY_STAT);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JsonUtils.fromJson(json, TodayStatVO.class);
    }

    public void saveTodayStat(TodayStatVO vo) {
        redisUtils.set(DataRedisKeyConstant.TODAY_STAT, JsonUtils.toJson(vo),
                dataProperties.getRealStatTtl(), TimeUnit.SECONDS);
    }

    public void saveYesterdayFallback(TodayStatVO vo) {
        redisUtils.set(DataRedisKeyConstant.YESTERDAY_STAT_FALLBACK, JsonUtils.toJson(vo),
                dataProperties.getRealStatTtl(), TimeUnit.SECONDS);
    }

    public TodayStatVO getYesterdayFallback() {
        String json = redisUtils.get(DataRedisKeyConstant.YESTERDAY_STAT_FALLBACK);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JsonUtils.fromJson(json, TodayStatVO.class);
    }

    public TodayStatVO initTodayIfAbsent() {
        TodayStatVO cached = getTodayStatFromCache();
        if (cached != null) {
            return cached;
        }
        TodayStatVO vo = emptyToday();
        saveTodayStat(vo);
        return vo;
    }

    public void incrementToday(int behaviorType) {
        TodayStatVO vo = initTodayIfAbsent();
        if (behaviorType == 1) {
            vo.setActiveUser(safeInc(vo.getActiveUser()));
        }
        if (behaviorType == 3) {
            vo.setOrderCount(safeInc(vo.getOrderCount()));
        }
        saveTodayStat(vo);
    }

    public void cacheGoodsSales(LocalDate date, String json) {
        redisUtils.set(DataRedisKeyConstant.GOODS_SALES_PREFIX + date, json,
                dataProperties.getRealStatTtl(), TimeUnit.SECONDS);
    }

    public String getGoodsSalesCache(LocalDate date) {
        return redisUtils.get(DataRedisKeyConstant.GOODS_SALES_PREFIX + date);
    }

    public void cacheUserActive(LocalDate date, String json) {
        redisUtils.set(DataRedisKeyConstant.USER_ACTIVE_PREFIX + date, json,
                dataProperties.getRealStatTtl(), TimeUnit.SECONDS);
    }

    private TodayStatVO emptyToday() {
        TodayStatVO vo = new TodayStatVO();
        vo.setStatDate(LocalDate.now());
        vo.setNewUser(0);
        vo.setActiveUser(0);
        vo.setOrderCount(0);
        vo.setOrderAmount(BigDecimal.ZERO);
        vo.setGroupSuccessNum(0);
        vo.setAfterSaleNum(0);
        vo.setMock(false);
        return vo;
    }

    private int safeInc(Integer value) {
        return value == null ? 1 : value + 1;
    }
}
