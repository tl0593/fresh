package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionQueryDTO {

    private Long userId;
    private BigDecimal orderAmount;
    private List<Long> catIds;
}
