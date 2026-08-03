package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionCalcVO {

    private List<UserCouponVO> availableCoupons;
    private FullReduceVO bestFullReduce;
    private BigDecimal maxCouponDeduct;
    private BigDecimal maxFullReduceDeduct;
}
