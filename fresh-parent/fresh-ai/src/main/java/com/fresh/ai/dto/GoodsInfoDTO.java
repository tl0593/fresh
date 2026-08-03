package com.fresh.ai.dto;

import lombok.Data;

@Data
public class GoodsInfoDTO {

    private Long goodsId;
    private String goodsName;
    private String spec;
    private String origin;
    private String extraInfo;
}
