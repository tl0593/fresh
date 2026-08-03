package com.fresh.user.dto;

import lombok.Data;

@Data
public class CartUpdateDTO {

    private Long goodsId;
    private Long specId;
    private Integer num;
    private Integer selected;
    /** true=在原有数量上累加（加购）；false/null=直接设为 num（购物车改数量） */
    private Boolean increment;
}
