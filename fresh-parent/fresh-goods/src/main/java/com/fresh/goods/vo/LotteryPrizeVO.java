package com.fresh.goods.vo;

import lombok.Data;

@Data
public class LotteryPrizeVO {

    private Long id;
    private Integer rewardType;
    private Integer rewardIntegral;
    private Long rewardCouponId;
    private String rewardName;
    private Integer weight;
    private Integer costIntegral;
}
