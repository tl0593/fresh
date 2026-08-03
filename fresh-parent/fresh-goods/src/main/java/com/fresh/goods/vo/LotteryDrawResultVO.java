package com.fresh.goods.vo;

import lombok.Data;

@Data
public class LotteryDrawResultVO {

    private Long prizeId;
    private Integer rewardType;
    private Integer rewardIntegral;
    private Long rewardCouponId;
    private String rewardName;
    private Integer costIntegral;
    private String message;
}
