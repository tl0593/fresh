package com.fresh.data.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBehaviorMqDTO {

    private Long userId;
    private Integer behaviorType;
    private Long goodsId;
    private LocalDateTime operateTime;
}
