package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill_coupon")
public class SeckillCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer startHour;
    private Integer totalStock;
    private Integer usedNum;
    private LocalDateTime activityStart;
    private LocalDateTime activityEnd;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
}
