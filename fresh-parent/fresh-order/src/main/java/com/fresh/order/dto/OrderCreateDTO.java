package com.fresh.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {

    private Long addressId;
    private Integer integralUsed;
    /** 用户优惠券 ID（user_coupon.id） */
    private Long userCouponId;
    private List<OrderItemDTO> items;
}
