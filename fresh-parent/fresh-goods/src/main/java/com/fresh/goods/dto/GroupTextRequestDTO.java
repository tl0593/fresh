package com.fresh.goods.dto;

import lombok.Data;

@Data
public class GroupTextRequestDTO {

    private Long goodsId;
    private String goodsName;
    private String spec;
    private String origin;
    private String extraInfo;
}
