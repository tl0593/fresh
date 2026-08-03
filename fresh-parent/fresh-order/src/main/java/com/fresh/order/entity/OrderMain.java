package com.fresh.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_main")
public class OrderMain {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer payType;
    private LocalDateTime payTime;
    private Integer status;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String community;
    private String detailAddress;
    private Long groupActivityId;
    private Long groupRecordId;
    private Long seckillActivityId;
    private Long couponId;
    private BigDecimal couponDeduct;
    private BigDecimal fullreduceDeduct;
    private Integer integralUsedCount;
    private BigDecimal integralDeductAmount;
    private Integer timeoutCancel;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
