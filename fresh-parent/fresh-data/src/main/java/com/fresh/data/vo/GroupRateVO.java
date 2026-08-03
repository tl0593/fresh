package com.fresh.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GroupRateVO {

    private LocalDate statDate;
    /** 成团率 = groupSuccessNum / orderCount */
    private BigDecimal groupSuccessRate;
    /** 售后占比 = afterSaleNum / orderCount */
    private BigDecimal afterSaleRate;
    private Integer orderCount;
    private Integer groupSuccessNum;
    private Integer afterSaleNum;
}
