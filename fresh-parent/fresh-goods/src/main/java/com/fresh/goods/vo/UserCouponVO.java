package com.fresh.goods.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long templateId;
    private String couponName;
    private BigDecimal fullAmount;
    private BigDecimal reduceAmount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validEnd;
    /** 0未使用 1已使用 2已过期 */
    private Integer useStatus;
}
