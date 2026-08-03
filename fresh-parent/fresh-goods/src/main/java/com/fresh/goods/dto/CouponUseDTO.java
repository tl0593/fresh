package com.fresh.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponUseDTO {

    private Long userId;
    private Long userCouponId;
    private String orderNo;
    private BigDecimal orderAmount;
}
