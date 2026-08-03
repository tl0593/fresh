package com.fresh.order.dto;

import lombok.Data;

@Data
public class GoodsPriceQueryDTO {

    private Long goodsId;
    private Long specId;
    private Integer activityType;
    private Long activityId;
}
