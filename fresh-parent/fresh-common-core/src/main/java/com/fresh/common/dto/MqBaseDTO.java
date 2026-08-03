package com.fresh.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MqBaseDTO implements Serializable {

    private String traceId;
    private Long operateUserId;
    private LocalDateTime operateTime;
}
