package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long goodsId;
    private Long specId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal price;
    private Integer num;
    private Integer activityType;
    private Long activityId;
}
