package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplateVO {

    private Long id;
    private String couponName;
    private Integer couponType;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer remainCount;
    private LocalDateTime endTime;
}
