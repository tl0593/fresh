package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionCalcVO {

    private List<UserCouponVO> availableCoupons;
    private BigDecimal maxCouponDeduct;
    private BigDecimal maxFullReduceDeduct;
}
