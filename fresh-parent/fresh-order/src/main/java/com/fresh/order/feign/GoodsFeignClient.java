package com.fresh.order.feign;

import com.fresh.common.base.Result;
import com.fresh.order.dto.CouponUseDTO;
import com.fresh.order.dto.GoodsPriceQueryDTO;
import com.fresh.order.dto.GoodsPriceVO;
import com.fresh.order.dto.PromotionCalcVO;
import com.fresh.order.dto.PromotionQueryDTO;
import com.fresh.order.dto.StockChangeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "fresh-goods", url = "${fresh.feign.goods-url:}")
public interface GoodsFeignClient {

    @PostMapping("/feign/stock/deduct")
    Result<Void> deductStock(@RequestBody StockChangeDTO dto);

    @PostMapping("/feign/stock/restore")
    Result<Void> restoreStock(@RequestBody StockChangeDTO dto);

    @PostMapping("/feign/goods/price")
    Result<GoodsPriceVO> resolvePrice(@RequestBody GoodsPriceQueryDTO dto);

    @PostMapping("/feign/promotion/calc")
    Result<PromotionCalcVO> calcPromotion(@RequestBody PromotionQueryDTO dto);

    @PostMapping("/feign/promotion/useCoupon")
    Result<BigDecimal> useCoupon(@RequestBody CouponUseDTO dto);
}
