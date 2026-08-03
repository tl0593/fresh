package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("integral_coupon")
public class IntegralCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer costIntegral;
    private Integer dailyLimit;
    private Integer totalStock;
    private Integer usedNum;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
}
