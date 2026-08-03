package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatVO {

    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer groupSuccessNum;
    private Integer afterSaleNum;
}
