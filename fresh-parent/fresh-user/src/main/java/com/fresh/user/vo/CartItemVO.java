package com.fresh.user.vo;

import lombok.Data;

@Data
public class CartItemVO {

    private Long goodsId;
    private Long specId;
    private Integer num;
    private Integer selected;
}
