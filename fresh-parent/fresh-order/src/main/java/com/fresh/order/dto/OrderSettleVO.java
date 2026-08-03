package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderSettleVO {

    /** 商品总额（优惠前） */
    private BigDecimal totalAmount;
    /** 应付金额（优惠后） */
    private BigDecimal payAmount;
    private Integer totalGoodsCount;
    private BigDecimal couponDeduct;
    private BigDecimal fullreduceDeduct;
    private Long selectedUserCouponId;
    private List<UserCouponVO> availableCoupons = new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long goodsId;
        private Long specId;
        private String goodsName;
        private String goodsImg;
        private BigDecimal price;
        private Integer num;
        private BigDecimal subTotal;
        private Integer activityType;
        private Long activityId;
    }
}
