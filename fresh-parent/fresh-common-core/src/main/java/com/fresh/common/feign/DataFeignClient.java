package com.fresh.common.feign;

import com.fresh.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "fresh-data")
public interface DataFeignClient {

    @GetMapping("/feign/stat/amount")
    Result<BigDecimal> getRangeAmount(@RequestParam("startDate") String startDate,
                                      @RequestParam("endDate") String endDate);

    @GetMapping("/feign/goods/sales")
    Result<Integer> getGoodsSales(@RequestParam("goodsId") Long goodsId,
                                  @RequestParam("startDate") String startDate,
                                  @RequestParam("endDate") String endDate);
}
