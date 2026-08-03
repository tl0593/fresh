package com.fresh.goods.controller.admin;

import com.fresh.common.base.PageVO;
import com.fresh.common.base.Result;
import com.fresh.goods.dto.CommentReplyDTO;
import com.fresh.goods.dto.GoodsSaveWithSpecsDTO;
import com.fresh.goods.dto.StockRestockDTO;
import com.fresh.goods.entity.*;
import com.fresh.goods.entity.promotion.*;
import com.fresh.goods.service.CommentService;
import com.fresh.goods.service.GoodsService;
import com.fresh.goods.service.PromotionService;
import com.fresh.goods.vo.CommentVO;
import com.fresh.goods.vo.GoodsAdminDetailVO;
import com.fresh.goods.vo.StockAlertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final GoodsService goodsService;
    private final CommentService commentService;
    private final PromotionService promotionService;

    // ===== 分类 =====
    @GetMapping("/admin/category/list")
    public Result<List<GoodsCategory>> categoryList() {
        return Result.success(goodsService.listAllCategories());
    }

    @PostMapping("/admin/category/save")
    public Result<Void> saveCategory(@RequestBody GoodsCategory category) {
        goodsService.saveCategory(category);
        return Result.success();
    }

    @DeleteMapping("/admin/category/{id}")
    public Result<Void> deleteCategory(@PathVariable("id") Long id) {
        goodsService.deleteCategory(id);
        return Result.success();
    }

    // ===== 商品 =====
    @GetMapping("/admin/goods/list")
    public Result<List<Goods>> goodsList() {
        return Result.success(goodsService.listAllGoods());
    }

    @PostMapping("/admin/goods/save")
    public Result<Void> saveGoods(@RequestBody Goods goods) {
        goodsService.saveGoods(goods);
        return Result.success();
    }

    /** 一次保存商品 + 规格（推荐） */
    @PostMapping("/admin/goods/saveWithSpecs")
    public Result<Map<String, Long>> saveGoodsWithSpecs(@RequestBody GoodsSaveWithSpecsDTO dto) {
        Long goodsId = goodsService.saveGoodsWithSpecs(dto);
        Map<String, Long> data = new HashMap<>(1);
        data.put("goodsId", goodsId);
        return Result.success(data);
    }

    @GetMapping("/admin/goods/{goodsId}/specs")
    public Result<List<GoodsSpec>> listSpecs(@PathVariable("goodsId") Long goodsId) {
        return Result.success(goodsService.listSpecsByGoodsId(goodsId));
    }

    @PostMapping("/admin/goods/spec/save")
    public Result<Void> saveSpec(@RequestBody GoodsSpec spec) {
        goodsService.saveSpec(spec);
        return Result.success();
    }

    @DeleteMapping("/admin/goods/spec/{id}")
    public Result<Void> deleteSpec(@PathVariable("id") Long id) {
        goodsService.deleteSpec(id);
        return Result.success();
    }

    @GetMapping("/admin/goods/{id}")
    public Result<GoodsAdminDetailVO> goodsDetail(@PathVariable("id") Long id) {
        return Result.success(goodsService.adminGoodsDetail(id));
    }

    @DeleteMapping("/admin/goods/{id}")
    public Result<Void> deleteGoods(@PathVariable("id") Long id) {
        goodsService.deleteGoods(id);
        return Result.success();
    }

    /** 缺货/低库存提醒列表 */
    @GetMapping("/admin/stock/alert")
    public Result<List<StockAlertVO>> stockAlert(
            @RequestParam(value = "threshold", required = false) Integer threshold,
            @RequestParam(value = "onlyOnSale", required = false, defaultValue = "true") Boolean onlyOnSale) {
        return Result.success(goodsService.listStockAlerts(threshold, onlyOnSale));
    }

    /** 待补货数量（菜单红点） */
    @GetMapping("/admin/stock/alertCount")
    public Result<Map<String, Object>> stockAlertCount(
            @RequestParam(value = "threshold", required = false) Integer threshold,
            @RequestParam(value = "onlyOnSale", required = false, defaultValue = "true") Boolean onlyOnSale) {
        Map<String, Object> data = new HashMap<>(2);
        data.put("count", goodsService.countStockAlerts(threshold, onlyOnSale));
        data.put("threshold", threshold);
        return Result.success(data);
    }

    /** 补货：增加规格库存并同步商品总库存 */
    @PostMapping("/admin/stock/restock")
    public Result<Void> restock(@RequestBody StockRestockDTO dto) {
        goodsService.restock(dto);
        return Result.success();
    }

    // ===== 团购 =====
    @GetMapping("/admin/group/list")
    public Result<List<GroupActivity>> groupList() {
        return Result.success(goodsService.listAllGroupActivities());
    }

    @PostMapping("/admin/group/save")
    public Result<Void> saveGroup(@RequestBody GroupActivity activity) {
        goodsService.saveGroupActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/admin/group/{id}")
    public Result<Void> deleteGroup(@PathVariable("id") Long id) {
        goodsService.deleteGroupActivity(id);
        return Result.success();
    }

    // ===== 秒杀 =====
    @GetMapping("/admin/seckill/list")
    public Result<List<SeckillActivity>> seckillList() {
        return Result.success(goodsService.listAllSeckillActivities());
    }

    @PostMapping("/admin/seckill/save")
    public Result<Void> saveSeckill(@RequestBody SeckillActivity activity) {
        goodsService.saveSeckillActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/admin/seckill/{id}")
    public Result<Void> deleteSeckill(@PathVariable("id") Long id) {
        goodsService.deleteSeckillActivity(id);
        return Result.success();
    }

    // ===== 评价 =====
    @GetMapping("/admin/comment/page")
    public Result<PageVO<CommentVO>> commentPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.adminPage(pageNum, pageSize));
    }

    @PutMapping("/admin/comment/hide/{commentId}")
    public Result<Void> hideComment(@PathVariable("commentId") Long commentId) {
        commentService.hide(commentId);
        return Result.success();
    }

    @PostMapping("/admin/comment/reply")
    public Result<Void> replyComment(@RequestBody CommentReplyDTO dto) {
        commentService.reply(dto);
        return Result.success();
    }

    // ===== 优惠券模板 =====
    @GetMapping("/admin/coupon/list")
    public Result<List<CouponTemplate>> couponList() {
        return Result.success(promotionService.listAllTemplates());
    }

    @PostMapping("/admin/coupon/save")
    public Result<Void> saveCoupon(@RequestBody CouponTemplate template) {
        promotionService.saveTemplate(template);
        return Result.success();
    }

    @DeleteMapping("/admin/coupon/{id}")
    public Result<Void> deleteCoupon(@PathVariable("id") Long id) {
        promotionService.deleteTemplate(id);
        return Result.success();
    }

    // ===== 满减 =====
    @GetMapping("/admin/fullreduce/list")
    public Result<List<FullReduceActivity>> fullReduceList() {
        return Result.success(promotionService.listFullReduce());
    }

    @PostMapping("/admin/fullreduce/save")
    public Result<Void> saveFullReduce(@RequestBody FullReduceActivity activity) {
        promotionService.saveFullReduce(activity);
        return Result.success();
    }

    @DeleteMapping("/admin/fullreduce/{id}")
    public Result<Void> deleteFullReduce(@PathVariable("id") Long id) {
        promotionService.deleteFullReduce(id);
        return Result.success();
    }

    // ===== 积分兑换券 =====
    @GetMapping("/admin/integralCoupon/list")
    public Result<List<IntegralCoupon>> integralCouponList() {
        return Result.success(promotionService.adminIntegralCouponList());
    }

    @PostMapping("/admin/integralCoupon/save")
    public Result<Void> saveIntegralCoupon(@RequestBody IntegralCoupon config) {
        promotionService.saveIntegralCoupon(config);
        return Result.success();
    }

    @DeleteMapping("/admin/integralCoupon/{id}")
    public Result<Void> deleteIntegralCoupon(@PathVariable("id") Long id) {
        promotionService.deleteIntegralCoupon(id);
        return Result.success();
    }

    // ===== 整点抢券 =====
    @GetMapping("/admin/seckillCoupon/list")
    public Result<List<SeckillCoupon>> seckillCouponList() {
        return Result.success(promotionService.adminSeckillCouponList());
    }

    @PostMapping("/admin/seckillCoupon/save")
    public Result<Void> saveSeckillCoupon(@RequestBody SeckillCoupon act) {
        promotionService.saveSeckillCoupon(act);
        return Result.success();
    }

    @DeleteMapping("/admin/seckillCoupon/{id}")
    public Result<Void> deleteSeckillCoupon(@PathVariable("id") Long id) {
        promotionService.deleteSeckillCoupon(id);
        return Result.success();
    }

    // ===== 抽奖奖品 =====
    @GetMapping("/admin/lotteryPrize/list")
    public Result<List<IntegralLotteryPrize>> lotteryPrizeList() {
        return Result.success(promotionService.listLotteryPrize());
    }

    @PostMapping("/admin/lotteryPrize/save")
    public Result<Void> saveLotteryPrize(@RequestBody IntegralLotteryPrize prize) {
        promotionService.saveLotteryPrize(prize);
        return Result.success();
    }

    @DeleteMapping("/admin/lotteryPrize/{id}")
    public Result<Void> deleteLotteryPrize(@PathVariable("id") Long id) {
        promotionService.deleteLotteryPrize(id);
        return Result.success();
    }

    @GetMapping("/admin/coupon/log")
    public Result<PageVO<CouponUseLog>> couponLog(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(promotionService.couponLogPage(pageNum, pageSize));
    }
}
