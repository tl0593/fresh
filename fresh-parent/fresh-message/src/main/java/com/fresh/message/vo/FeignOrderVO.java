package com.fresh.message.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeignOrderVO {

    private String orderNo;
    private BigDecimal payAmount;
    private Long userId;
}
