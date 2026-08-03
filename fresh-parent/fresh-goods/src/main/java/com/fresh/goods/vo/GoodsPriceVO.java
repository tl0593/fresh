package com.fresh.goods.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsPriceVO {

    private Long goodsId;
    private Long specId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal price;
    private Integer activityType;
    private Long activityId;
}
