package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("integral_lottery_prize")
public class IntegralLotteryPrize {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer rewardType;
    private Integer rewardIntegral;
    private Long rewardCouponId;
    private Integer weight;
    private Integer costIntegral;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
}
