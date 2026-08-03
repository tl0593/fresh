package com.fresh.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sale")
public class AfterSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderItemId;
    private Long userId;
    private Long goodsId;
    private String damageImg;
    private Integer aiDamageLevel;
    private BigDecimal aiRate;
    private BigDecimal aiRefundMoney;
    private BigDecimal actualRefundMoney;
    private Integer auditStatus;
    private Long auditAdminId;
    private LocalDateTime refundTime;
    private String remark;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
