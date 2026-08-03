package com.fresh.goods.dto;

import lombok.Data;

@Data
public class StockChangeDTO {

    private Long goodsId;
    private Long specId;
    private Integer num;
    private Integer activityType;
    private Long activityId;
}
