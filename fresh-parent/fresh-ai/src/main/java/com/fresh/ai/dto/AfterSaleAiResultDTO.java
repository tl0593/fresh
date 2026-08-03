package com.fresh.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleAiResultDTO {

    private Long afterSaleId;
    private Integer aiDamageLevel;
    private BigDecimal aiRate;
    private BigDecimal aiRefundMoney;
}
