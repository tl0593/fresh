package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillCouponVO {

    private Long id;
    private Long templateId;
    private String couponName;
    private Integer couponType;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer startHour;
    private Integer totalStock;
    private Integer usedNum;
    private Integer remainStock;
    private LocalDateTime activityStart;
    private LocalDateTime activityEnd;
    private Integer status;
    /** 0未开始 1开抢中 2已结束/非本小时 */
    private Integer grabStatus;
    private String grabStatusText;
}
