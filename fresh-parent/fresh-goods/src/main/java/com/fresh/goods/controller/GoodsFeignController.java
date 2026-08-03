package com.fresh.goods.controller;

import com.fresh.common.base.Result;
import com.fresh.goods.dto.IntegralExchangeDTO;
import com.fresh.goods.dto.CouponUseDTO;
import com.fresh.goods.dto.GoodsPriceQueryDTO;
import com.fresh.goods.dto.PromotionQueryDTO;
import com.fresh.goods.dto.SeckillCouponDTO;
import com.fresh.goods.dto.StockChangeDTO;
import com.fresh.goods.service.CommentService;
import com.fresh.goods.service.GoodsService;
import com.fresh.goods.service.PromotionService;
import com.fresh.goods.vo.CommentRateVO;
import com.fresh.goods.vo.GoodsPriceVO;
import com.fresh.goods.vo.LotteryPrizeVO;
import com.fresh.goods.vo.PromotionCalcVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/feign")
@RequiredArgsConstructor
public class GoodsFeignController {

    private final GoodsService goodsService;
    private final CommentService commentService;
    private final PromotionService promotionService;

    @PostMapping("/stock/deduct")
    public Result<Void> deductStock(@RequestBody StockChangeDTO dto) {
        goodsService.deductStock(dto);
        return Result.success();
    }

    @PostMapping("/stock/restore")
    public Result<Void> restoreStock(@RequestBody StockChangeDTO dto) {
        goodsService.restoreStock(dto);
        return Result.success();
    }

    @GetMapping("/comment/rate/{goodsId}")
    public Result<CommentRateVO> getGoodsCommentRate(@PathVariable("goodsId") Long goodsId) {
        return Result.success(commentService.getCommentRate(goodsId));
    }

    @PostMapping("/integral/exchange")
    public Result<Void> exchangeCoupon(@RequestBody IntegralExchangeDTO dto) {
        promotionService.exchangeCoupon(dto);
        return Result.success();
    }

    @GetMapping("/lottery/prize")
    public Result<List<LotteryPrizeVO>> getLotteryPrize() {
        return Result.success(promotionService.lotteryPrizeList());
    }

    @PostMapping("/seckill/coupon/receive")
    public Result<Void> seckillReceive(@RequestBody SeckillCouponDTO dto) {
        promotionService.seckillReceive(dto);
        return Result.success();
    }

    @PostMapping("/promotion/calc")
    public Result<PromotionCalcVO> getAvailablePromotion(@RequestBody PromotionQueryDTO dto) {
        return Result.success(promotionService.calcPromotion(dto));
    }

    @PostMapping("/promotion/useCoupon")
    public Result<BigDecimal> useCoupon(@RequestBody CouponUseDTO dto) {
        return Result.success(promotionService.useCoupon(dto));
    }

    /** 订单服务计价：规格价 / 团购价 / 秒杀价 */
    @PostMapping("/goods/price")
    public Result<GoodsPriceVO> resolvePrice(@RequestBody GoodsPriceQueryDTO dto) {
        return Result.success(goodsService.resolvePrice(dto));
    }
}
