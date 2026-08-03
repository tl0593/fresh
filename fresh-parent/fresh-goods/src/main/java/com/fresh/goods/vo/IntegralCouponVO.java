package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IntegralCouponVO {

    private Long id;
    private Long templateId;
    private String couponName;
    private Integer couponType;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer costIntegral;
    private Integer dailyLimit;
    private Integer totalStock;
    private Integer usedNum;
    private Integer remainStock;
    private Integer status;
}
