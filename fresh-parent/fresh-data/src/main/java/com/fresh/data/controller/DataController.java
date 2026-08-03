package com.fresh.data.controller;

import com.fresh.common.base.Result;
import com.fresh.data.entity.DailyStat;
import com.fresh.data.entity.GoodsSalesStat;
import com.fresh.data.service.DataService;
import com.fresh.data.vo.GroupRateVO;
import com.fresh.data.vo.TodayStatVO;
import com.fresh.data.vo.UserTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping("/stat/today")
    public Result<TodayStatVO> today() {
        return Result.success(dataService.todayStat());
    }

    @GetMapping("/stat/daily/list")
    public Result<List<DailyStat>> dailyList(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        return Result.success(dataService.dailyList(startDate, endDate));
    }

    @GetMapping("/stat/goods/sales")
    public Result<List<GoodsSalesStat>> goodsSales(
            @RequestParam(value = "statDate", required = false) String statDate,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(dataService.goodsSales(statDate, limit));
    }

    @GetMapping("/stat/user/trend")
    public Result<UserTrendVO> userTrend(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        return Result.success(dataService.userTrend(startDate, endDate));
    }

    @GetMapping("/stat/group/rate")
    public Result<GroupRateVO> groupRate(
            @RequestParam(value = "statDate", required = false) String statDate) {
        return Result.success(dataService.groupRate(statDate));
    }
}
