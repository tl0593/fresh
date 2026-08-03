package com.fresh.goods.dto;

import lombok.Data;

@Data
public class OrderItemCheckVO {

    private Long orderItemId;
    private Long goodsId;
    private Long specId;
    private String orderNo;
    private Long userId;
    private Integer orderStatus;
    private Integer isCommented;
    private Boolean canComment;
}
