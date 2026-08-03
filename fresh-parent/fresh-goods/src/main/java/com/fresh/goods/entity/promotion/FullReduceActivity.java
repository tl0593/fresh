package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("full_reduce_activity")
public class FullReduceActivity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityName;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    private Integer targetType;
    private String targetCatIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer stackCoupon;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
}
