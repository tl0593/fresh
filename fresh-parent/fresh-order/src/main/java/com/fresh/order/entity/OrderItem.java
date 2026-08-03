package com.fresh.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long goodsId;
    private Long specId;
    private Integer activityType;
    private Long activityId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal price;
    private Integer num;
    private BigDecimal subTotal;
    private Integer isCommented;
    private LocalDateTime createTime;
}
