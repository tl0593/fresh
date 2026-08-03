package com.fresh.data.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatDTO {

    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer groupSuccessNum;
    private Integer afterSaleNum;
}
