package com.fresh.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.goods")
public class GoodsProperties {

    private long goodsCacheTtl = 1800;
    private long categoryCacheTtl = -1;
    private long lockWaitTime = 3;
    private long lockHoldTime = 10;
    private String hotListCron = "0 */10 * * * ?";
    private long commentCacheTtl = 1800;
    private String commentRateCron = "0 */15 * * * ?";
    private long couponTemplateTtl = 900;
    private long userCouponTtl = 600;
    private String promotionCron = "0 0 1 * * ?";
    private String seckillCouponCron = "0 59 * * * ?";
    /** 库存预警阈值：规格库存 <= 该值时提醒补货 */
    private int stockWarnThreshold = 10;
}
