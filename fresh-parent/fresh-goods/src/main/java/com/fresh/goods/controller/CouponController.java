package com.fresh.goods.controller;

import com.fresh.common.base.Result;
import com.fresh.common.util.ContextUtil;
import com.fresh.goods.dto.CouponReceiveDTO;
import com.fresh.goods.dto.IntegralExchangeDTO;
import com.fresh.goods.dto.SeckillCouponDTO;
import com.fresh.goods.service.PromotionService;
import com.fresh.goods.vo.BatchReceiveResultVO;
import com.fresh.goods.vo.CouponTemplateVO;
import com.fresh.goods.vo.IntegralCouponVO;
import com.fresh.goods.vo.LotteryDrawResultVO;
import com.fresh.goods.vo.LotteryPrizeVO;
import com.fresh.goods.vo.SeckillCouponVO;
import com.fresh.goods.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final PromotionService promotionService;

    @GetMapping("/coupon/template/list")
    public Result<List<CouponTemplateVO>> templateList() {
        return Result.success(promotionService.templateList());
    }

    @PostMapping("/coupon/receive")
    public Result<Void> receive(@RequestBody CouponReceiveDTO dto) {
        if (dto.getUserId() == null) {
            dto.setUserId(ContextUtil.getUserId());
        }
        promotionService.receive(dto);
        return Result.success();
    }

    /** 一键领取当前可领优惠券 */
    @PostMapping("/coupon/receive/batch")
    public Result<BatchReceiveResultVO> receiveBatch() {
        return Result.success(promotionService.receiveBatch());
    }

    /** 我的优惠券：status 0未使用 1已使用 2已过期，不传则全部 */
    @GetMapping("/coupon/mine")
    public Result<List<UserCouponVO>> mine(@RequestParam(value = "status", required = false) Integer status) {
        return Result.success(promotionService.myCoupons(status));
    }

    @GetMapping("/coupon/seckill/list")
    public Result<List<SeckillCouponVO>> seckillList() {
        return Result.success(promotionService.seckillCouponVoList());
    }

    @PostMapping("/coupon/seckill/receive")
    public Result<Void> seckillReceive(@RequestBody SeckillCouponDTO dto) {
        if (dto.getUserId() == null) {
            dto.setUserId(ContextUtil.getUserId());
        }
        promotionService.seckillReceive(dto);
        return Result.success();
    }

    @GetMapping("/integral/coupon/list")
    public Result<List<IntegralCouponVO>> integralList() {
        return Result.success(promotionService.integralCouponVoList());
    }

    @PostMapping("/integral/coupon/exchange")
    public Result<Void> exchange(@RequestBody IntegralExchangeDTO dto) {
        if (dto.getUserId() == null) {
            dto.setUserId(ContextUtil.getUserId());
        }
        promotionService.exchangeCoupon(dto);
        return Result.success();
    }

    @GetMapping("/integral/lottery/prizes")
    public Result<List<LotteryPrizeVO>> lotteryPrizes() {
        return Result.success(promotionService.lotteryPrizeList());
    }

    @PostMapping("/integral/lottery/draw")
    public Result<LotteryDrawResultVO> lotteryDraw() {
        return Result.success(promotionService.lotteryDraw());
    }
}
