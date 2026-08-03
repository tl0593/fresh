package com.fresh.goods.vo;

import lombok.Data;

@Data
public class CommentRateVO {

    private Long goodsId;
    private Double avgScore;
    private Double goodRate;
    private Long totalCount;
}
