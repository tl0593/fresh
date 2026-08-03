package com.fresh.goods.service;

import com.fresh.goods.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoodsScheduledTask {

    private final GoodsService goodsService;
    private final PromotionService promotionService;

    @Scheduled(cron = "${fresh.goods.hot-list-cron:0 */10 * * * ?}")
    public void refreshHotList() {
        goodsService.refreshHotListCache();
    }

    @Scheduled(cron = "${fresh.goods.comment-rate-cron:0 */15 * * * ?}")
    public void refreshCommentRate() {
        // 好评率按需懒加载，定时任务预留扩展点
    }

    @Scheduled(cron = "${fresh.goods.promotion-cron:0 0 1 * * ?}")
    public void cleanExpiredCoupons() {
        promotionService.cleanExpiredCoupons();
    }

    @Scheduled(cron = "${fresh.goods.seckill-coupon-cron:0 59 * * * ?}")
    public void warmupSeckillCoupon() {
        promotionService.warmupSeckillCouponStock();
    }
}
