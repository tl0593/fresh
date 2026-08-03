package com.fresh.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.data.config.DataProperties;
import com.fresh.data.dto.GoodsSalesItemDTO;
import com.fresh.data.entity.DailyStat;
import com.fresh.data.entity.GoodsSalesStat;
import com.fresh.data.mapper.DailyStatMapper;
import com.fresh.data.mapper.GoodsSalesStatMapper;
import com.fresh.data.vo.GroupRateVO;
import com.fresh.data.vo.TodayStatVO;
import com.fresh.data.vo.UserTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final DailyStatMapper dailyStatMapper;
    private final GoodsSalesStatMapper goodsSalesStatMapper;
    private final DataStatCacheService dataStatCacheService;
    private final DataProperties dataProperties;
    private final DailyStatAggregateService dailyStatAggregateService;

    public TodayStatVO todayStat() {
        LocalDate today = LocalDate.now();
        try {
            AggregateSnapshot snap = dailyStatAggregateService.aggregateForDate(today);
            if (snap != null && snap.getDaily() != null) {
                TodayStatVO vo = toTodayVo(snap.getDaily());
                vo.setMock(false);
                dataStatCacheService.saveTodayStat(vo);
                return vo;
            }
        } catch (Exception e) {
            log.warn("today live aggregate failed", e);
        }

        DailyStat stat = dailyStatMapper.selectOne(new LambdaQueryWrapper<DailyStat>()
                .eq(DailyStat::getStatDate, today)
                .last("LIMIT 1"));
        if (stat != null) {
            TodayStatVO vo = toTodayVo(stat);
            vo.setMock(false);
            dataStatCacheService.saveTodayStat(vo);
            return vo;
        }

        TodayStatVO cached = dataStatCacheService.getTodayStatFromCache();
        if (cached != null) {
            cached.setMock(false);
            return cached;
        }

        return dataStatCacheService.initTodayIfAbsent();
    }

    public List<DailyStat> dailyList(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().minusDays(30));
        LocalDate end = parseDate(endDate, LocalDate.now());
        validateRange(start, end);
        // 补齐今日，保证看板趋势含最新一天
        try {
            dailyStatAggregateService.aggregateForDate(LocalDate.now());
        } catch (Exception e) {
            log.warn("dailyList today aggregate failed", e);
        }
        return dailyStatMapper.selectList(new LambdaQueryWrapper<DailyStat>()
                .ge(DailyStat::getStatDate, start)
                .le(DailyStat::getStatDate, end)
                .orderByAsc(DailyStat::getStatDate));
    }

    public List<GoodsSalesStat> goodsSales(String statDate, Integer limit) {
        LocalDate date = StringUtils.hasText(statDate) ? LocalDate.parse(statDate) : LocalDate.now();
        int top = limit == null || limit <= 0 ? 10 : limit;
        try {
            AggregateSnapshot snap = dailyStatAggregateService.aggregateForDate(date);
            if (snap != null && snap.getGoodsItems() != null && !snap.getGoodsItems().isEmpty()) {
                return toSalesRows(date, snap.getGoodsItems(), top);
            }
        } catch (Exception e) {
            log.warn("goodsSales live aggregate failed for {}", date, e);
        }
        return goodsSalesStatMapper.selectList(new LambdaQueryWrapper<GoodsSalesStat>()
                .eq(GoodsSalesStat::getStatDate, date)
                .orderByDesc(GoodsSalesStat::getSaleNum)
                .last("LIMIT " + top));
    }

    public UserTrendVO userTrend(String startDate, String endDate) {
        List<DailyStat> list = dailyList(startDate, endDate);
        UserTrendVO vo = new UserTrendVO();
        for (DailyStat stat : list) {
            UserTrendVO.Item item = new UserTrendVO.Item();
            item.setStatDate(stat.getStatDate());
            item.setNewUser(stat.getNewUser());
            item.setActiveUser(stat.getActiveUser());
            vo.getPoints().add(item);
        }
        return vo;
    }

    public GroupRateVO groupRate(String statDate) {
        LocalDate date = StringUtils.hasText(statDate) ? LocalDate.parse(statDate) : LocalDate.now();
        DailyStat source = null;
        try {
            AggregateSnapshot snap = dailyStatAggregateService.aggregateForDate(date);
            if (snap != null) {
                source = snap.getDaily();
            }
        } catch (Exception e) {
            log.warn("groupRate live aggregate failed for {}", date, e);
        }
        if (source == null) {
            source = dailyStatMapper.selectOne(new LambdaQueryWrapper<DailyStat>()
                    .eq(DailyStat::getStatDate, date)
                    .last("LIMIT 1"));
        }
        GroupRateVO vo = new GroupRateVO();
        vo.setStatDate(date);
        if (source == null) {
            vo.setOrderCount(0);
            vo.setGroupSuccessNum(0);
            vo.setAfterSaleNum(0);
            vo.setGroupSuccessRate(BigDecimal.ZERO);
            vo.setAfterSaleRate(BigDecimal.ZERO);
            return vo;
        }
        vo.setOrderCount(defaultInt(source.getOrderCount()));
        vo.setGroupSuccessNum(defaultInt(source.getGroupSuccessNum()));
        vo.setAfterSaleNum(defaultInt(source.getAfterSaleNum()));
        vo.setGroupSuccessRate(rate(vo.getGroupSuccessNum(), vo.getOrderCount()));
        vo.setAfterSaleRate(rate(vo.getAfterSaleNum(), vo.getOrderCount()));
        return vo;
    }

    public BigDecimal rangeAmount(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, LocalDate.now().minusDays(7));
        LocalDate end = parseDate(endDate, LocalDate.now());
        validateRange(start, end);
        return dailyStatAggregateService.sumOrderAmount(start, end);
    }

    public Integer goodsSalesRange(Long goodsId, String startDate, String endDate) {
        if (goodsId == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "goodsId不能为空");
        }
        LocalDate start = parseDate(startDate, LocalDate.now().minusDays(7));
        LocalDate end = parseDate(endDate, LocalDate.now());
        validateRange(start, end);
        return dailyStatAggregateService.sumGoodsSales(goodsId, start, end);
    }

    private List<GoodsSalesStat> toSalesRows(LocalDate date, List<GoodsSalesItemDTO> items, int top) {
        List<GoodsSalesStat> rows = new ArrayList<>();
        items.stream()
                .filter(i -> i.getGoodsId() != null)
                .sorted(Comparator.comparing((GoodsSalesItemDTO i) -> defaultInt(i.getSaleNum())).reversed())
                .limit(top)
                .forEach(item -> {
                    GoodsSalesStat row = new GoodsSalesStat();
                    row.setStatDate(date);
                    row.setGoodsId(item.getGoodsId());
                    row.setSaleNum(defaultInt(item.getSaleNum()));
                    row.setSaleAmount(defaultAmount(item.getSaleAmount()));
                    row.setGoodsName(item.getGoodsName());
                    rows.add(row);
                });
        return rows;
    }

    private TodayStatVO toTodayVo(DailyStat stat) {
        TodayStatVO vo = new TodayStatVO();
        BeanUtils.copyProperties(stat, vo);
        return vo;
    }

    private LocalDate parseDate(String text, LocalDate defaultDate) {
        if (!StringUtils.hasText(text)) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "日期格式错误，请使用 yyyy-MM-dd");
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days > dataProperties.getMaxQueryDays()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(),
                    "查询区间不能超过 " + dataProperties.getMaxQueryDays() + " 天");
        }
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
