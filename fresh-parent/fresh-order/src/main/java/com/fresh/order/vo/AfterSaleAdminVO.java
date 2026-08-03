package com.fresh.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleAdminVO {

    private Long id;
    private Long orderItemId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal itemPrice;
    private Integer itemNum;
    private String damageImg;
    private Integer aiDamageLevel;
    private BigDecimal aiRate;
    private BigDecimal aiRefundMoney;
    private BigDecimal actualRefundMoney;
    private Integer auditStatus;
    private Long auditAdminId;
    private LocalDateTime refundTime;
    private String remark;
    private LocalDateTime createTime;
    private Integer orderStatus;
}
