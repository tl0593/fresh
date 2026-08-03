package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_use_log")
public class CouponUseLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long templateId;
    private Long userId;
    private String orderNo;
    private BigDecimal deductMoney;
    private LocalDateTime createTime;
}
