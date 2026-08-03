package com.fresh.order.dto;

import lombok.Data;

@Data
public class StockChangeDTO {

    private Long goodsId;
    private Long specId;
    private Integer num;
    private Integer activityType;
    private Long activityId;
}
