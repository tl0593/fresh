package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsSalesItemVO {

    private Long goodsId;
    private String goodsName;
    private Integer saleNum;
    private BigDecimal saleAmount;
}
