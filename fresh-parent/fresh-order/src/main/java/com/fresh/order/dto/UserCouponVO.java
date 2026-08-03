package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserCouponVO {

    private Long id;
    private Long templateId;
    private String couponName;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    /** 用字符串接 Feign，避免 goods 的 "yyyy-MM-dd HH:mm:ss" 反序列化失败 */
    private String validEnd;
    private Integer useStatus;
}
