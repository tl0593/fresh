package com.fresh.goods.dto;

import lombok.Data;

@Data
public class UserIntegralDTO {

    private Long userId;
    private Integer integral;
    private Long orderId;
    private String remark;
}
