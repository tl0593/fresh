package com.fresh.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TodayStatVO {

    private LocalDate statDate;
    private Integer newUser;
    private Integer activeUser;
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer groupSuccessNum;
    private Integer afterSaleNum;
    private Boolean mock;
}
