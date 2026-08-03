package com.fresh.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ImageDamageResultDTO {

    private Integer damageLevel;
    private BigDecimal damageRatio;
    private BigDecimal refundAmount;
    private String description;
}
