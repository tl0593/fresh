package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FullReduceVO {

    private Long id;
    private String activityName;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer stackCoupon;
}
