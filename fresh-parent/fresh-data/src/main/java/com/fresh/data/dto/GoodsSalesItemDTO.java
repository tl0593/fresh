package com.fresh.data.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsSalesItemDTO {

    private Long goodsId;
    private String goodsName;
    private Integer saleNum;
    private BigDecimal saleAmount;
}
