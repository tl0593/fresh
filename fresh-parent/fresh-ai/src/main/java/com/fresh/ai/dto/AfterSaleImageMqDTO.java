package com.fresh.ai.dto;

import lombok.Data;

@Data
public class AfterSaleImageMqDTO {

    private Long afterSaleId;
    private String imgUrl;
    private Long userId;
    private Long goodsId;
}
