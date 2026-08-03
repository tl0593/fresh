package com.fresh.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.base.Result;
import com.fresh.common.util.JsonUtils;
import com.fresh.data.dto.GoodsSalesItemDTO;
import com.fresh.data.dto.OrderStatDTO;
import com.fresh.data.dto.StatQueryDTO;
import com.fresh.data.dto.UserStatDTO;
import com.fresh.data.entity.DailyStat;
import com.fresh.data.entity.GoodsSalesStat;
import com.fresh.data.feign.GoodsStatFeignClient;
import com.fresh.data.feign.OrderStatFeignClient;
import com.fresh.data.feign.UserStatFeignClient;
import com.fresh.data.mapper.DailyStatMapper;
import com.fresh.data.mapper.GoodsSalesStatMapper;
import com.fresh.data.mapper.UserBehaviorLogMapper;
import com.fresh.data.vo.TodayStatVO;
import com.fresh.data.vo.UserTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStatAggregateService {

    private final DailyStatMapper dailyStatMapper;
    private final GoodsSalesStatMapper goodsSalesStatMapper;
    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final OrderStatFeignClient orderStatFeignClient;
    private final UserStatFeignClient userStatFeignClient;
    private final GoodsStatFeignClient goodsStatFeignClient;
    private final DataStatCacheService dataStatCacheService;

    private final ConcurrentHashMap<LocalDate, Object> dateLocks = new ConcurrentHashMap<>();

    /** 按日聚合订单/用户真实数据，幂等落库；并发时同日期串行 */
    public AggregateSnapshot aggregateForDate(LocalDate statDate) {
        Object lock = dateLocks.computeIfAbsent(statDate, d -> new Object());
        synchronized (lock) {
            return doAggregate(statDate);
        }
    }

    private AggregateSnapshot doAggregate(LocalDate statDate) {
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.plusDays(1).atStartOfDay();

        OrderStatDTO orderStat = fetchOrderStat(statDate);
        UserStatDTO userStat = fetchUserStat(statDate);
        mergeBehaviorFallback(statDate, start, end, orderStat, userStat);

        DailyStat daily = new DailyStat();
        daily.setStatDate(statDate);
        daily.setNewUser(defaultInt(userStat.getNewUser()));
        daily.setActiveUser(defaultInt(userStat.getActiveUser()));
        daily.setOrderCount(defaultInt(orderStat.getOrderCount()));
        daily.setOrderAmount(defaultAmount(orderStat.getOrderAmount()));
        daily.setGroupSuccessNum(defaultInt(orderStat.getGroupSuccessNum()));
        daily.setAfterSaleNum(defaultInt(orderStat.getAfterSaleNum()));

        try {
            dailyStatMapper.upsert(daily);
            DailyStat saved = dailyStatMapper.selectOne(new LambdaQueryWrapper<DailyStat>()
                    .eq(DailyStat::getStatDate, statDate)
                    .last("LIMIT 1"));
            if (saved != null) {
                daily = saved;
            }
        } catch (Exception e) {
            log.warn("persist daily_stat failed for {}, still return live feign data", statDate, e);
        }

        List<GoodsSalesItemDTO> goodsItems = fetchGoodsSales(statDate, start, end);
        for (GoodsSalesItemDTO item : goodsItems) {
            if (item.getGoodsId() == null) {
                continue;
            }
            GoodsSalesStat row = new GoodsSalesStat();
            row.setStatDate(statDate);
            row.setGoodsId(item.getGoodsId());
            row.setSaleNum(defaultInt(item.getSaleNum()));
            row.setSaleAmount(defaultAmount(item.getSaleAmount()));
            try {
                goodsSalesStatMapper.upsert(row);
            } catch (Exception e) {
                log.warn("persist goods_sales_stat failed date={} goodsId={}", statDate, item.getGoodsId(), e);
            }
        }

        refreshCaches(statDate, daily, goodsItems);
        log.info("daily stat aggregated for {} orders={} amount={}",
                statDate, daily.getOrderCount(), daily.getOrderAmount());

        AggregateSnapshot snapshot = new AggregateSnapshot();
        snapshot.setDaily(daily);
        snapshot.setGoodsItems(goodsItems);
        return snapshot;
    }

    private OrderStatDTO fetchOrderStat(LocalDate statDate) {
        OrderStatDTO fallback = new OrderStatDTO();
        fallback.setOrderCount(0);
        fallback.setOrderAmount(BigDecimal.ZERO);
        fallback.setGroupSuccessNum(0);
        fallback.setAfterSaleNum(0);
        try {
            StatQueryDTO query = new StatQueryDTO();
            query.setStartDate(statDate);
            query.setEndDate(statDate);
            Result<OrderStatDTO> result = orderStatFeignClient.batchStat(query);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("order feign stat failed for {}, use behavior fallback", statDate, e);
        }
        return fallback;
    }

    private UserStatDTO fetchUserStat(LocalDate statDate) {
        UserStatDTO fallback = new UserStatDTO();
        fallback.setNewUser(0);
        fallback.setActiveUser(0);
        try {
            Result<UserStatDTO> result = userStatFeignClient.dailyStat(statDate.toString());
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("user feign stat failed for {}, use behavior fallback", statDate, e);
        }
        return fallback;
    }

    private void mergeBehaviorFallback(LocalDate statDate, LocalDateTime start, LocalDateTime end,
                                       OrderStatDTO orderStat, UserStatDTO userStat) {
        Integer active = userBehaviorLogMapper.countActiveUsers(start, end);
        if (active != null && active > defaultInt(userStat.getActiveUser())) {
            userStat.setActiveUser(active);
        }
        Integer newUsers = userBehaviorLogMapper.countNewUsers(statDate);
        if (newUsers != null && newUsers > defaultInt(userStat.getNewUser())) {
            userStat.setNewUser(newUsers);
        }
        Integer orderEvents = userBehaviorLogMapper.countByBehaviorType(3, start, end);
        if (orderEvents != null && orderEvents > defaultInt(orderStat.getOrderCount())) {
            orderStat.setOrderCount(orderEvents);
        }
    }

    private List<GoodsSalesItemDTO> fetchGoodsSales(LocalDate statDate, LocalDateTime start, LocalDateTime end) {
        try {
            StatQueryDTO query = new StatQueryDTO();
            query.setStartDate(statDate);
            query.setEndDate(statDate);
            Result<List<GoodsSalesItemDTO>> result = goodsStatFeignClient.dailySalesStat(query);
            if (result != null && !CollectionUtils.isEmpty(result.getData())) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("goods feign sales stat failed for {}, use behavior fallback", statDate, e);
        }
        List<GoodsSalesItemDTO> local = userBehaviorLogMapper.aggregateGoodsSales(start, end);
        return local == null ? List.of() : local;
    }

    private void refreshCaches(LocalDate statDate, DailyStat daily, List<GoodsSalesItemDTO> goodsItems) {
        TodayStatVO vo = new TodayStatVO();
        BeanUtils.copyProperties(daily, vo);
        vo.setMock(false);
        dataStatCacheService.saveYesterdayFallback(vo);

        UserTrendVO.Item userPoint = new UserTrendVO.Item();
        userPoint.setStatDate(statDate);
        userPoint.setNewUser(daily.getNewUser());
        userPoint.setActiveUser(daily.getActiveUser());
        dataStatCacheService.cacheUserActive(statDate, JsonUtils.toJson(userPoint));

        if (!CollectionUtils.isEmpty(goodsItems)) {
            dataStatCacheService.cacheGoodsSales(statDate, JsonUtils.toJson(goodsItems));
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal sumOrderAmount(LocalDate start, LocalDate end) {
        List<DailyStat> list = dailyStatMapper.selectList(new LambdaQueryWrapper<DailyStat>()
                .ge(DailyStat::getStatDate, start)
                .le(DailyStat::getStatDate, end));
        return list.stream()
                .map(d -> d.getOrderAmount() == null ? BigDecimal.ZERO : d.getOrderAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int sumGoodsSales(Long goodsId, LocalDate start, LocalDate end) {
        List<GoodsSalesStat> list = goodsSalesStatMapper.selectList(new LambdaQueryWrapper<GoodsSalesStat>()
                .eq(GoodsSalesStat::getGoodsId, goodsId)
                .ge(GoodsSalesStat::getStatDate, start)
                .le(GoodsSalesStat::getStatDate, end));
        return list.stream().mapToInt(g -> g.getSaleNum() == null ? 0 : g.getSaleNum()).sum();
    }
}
