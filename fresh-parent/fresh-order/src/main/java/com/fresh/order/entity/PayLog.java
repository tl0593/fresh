package com.fresh.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_log")
public class PayLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private String outTradeNo;
    private BigDecimal payAmount;
    private Integer payStatus;
    private String callbackContent;
    private LocalDateTime createTime;
}
