package com.fresh.order.dto;

import lombok.Data;

@Data
public class IntegralDTO {

    private Long userId;
    private Integer integral;
    private Long orderId;
    private String remark;
}
