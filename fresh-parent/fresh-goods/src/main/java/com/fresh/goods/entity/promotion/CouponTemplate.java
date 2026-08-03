package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponName;
    private Integer couponType;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer totalCount;
    private Integer usedCount;
    private Integer validDay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer limitType;
    private Integer limitNum;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
