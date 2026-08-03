package com.fresh.data.controller;

import com.fresh.common.base.Result;
import com.fresh.data.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/feign")
@RequiredArgsConstructor
public class DataFeignController {

    private final DataService dataService;

    @GetMapping("/stat/amount")
    public Result<BigDecimal> rangeAmount(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        return Result.success(dataService.rangeAmount(startDate, endDate));
    }

    @GetMapping("/goods/sales")
    public Result<Integer> goodsSales(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        return Result.success(dataService.goodsSalesRange(goodsId, startDate, endDate));
    }
}
